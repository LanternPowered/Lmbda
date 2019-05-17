/*
 * This file is part of Lmbda, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.lmbda;

import static java.util.Objects.requireNonNull;
import static org.lanternpowered.lmbda.InternalUtilities.doUnchecked;
import static org.lanternpowered.lmbda.InternalUtilities.getPackageName;
import static org.lanternpowered.lmbda.InternalUtilities.throwUnchecked;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * Internal method handles.
 */
final class InternalMethodHandles {

    static final @NonNull Adapter adapter = loadAdapter();

    /**
     * Loads the appropriate {@link Adapter}.
     *
     * @return The adapter
     */
    private static @NonNull Adapter loadAdapter() {
        return AccessController.doPrivileged((PrivilegedAction<Adapter>) () -> {
            if (isJava9Available()) {
                return new Java9Adapter();
            }
            return new Java8Adapter();
        });
    }

    /**
     * Represents a adapter. The implementation depends on the Java version.
     */
    interface Adapter {

        /**
         * Gets a lookup object with full capabilities to emulate all supported bytecode
         * behaviors, including private access, on a target class.
         *
         * @param targetClass The target class for which private access should be acquired
         * @param lookup The caller lookup object
         * @return A lookup object for the target class, with private access
         * @throws IllegalAccessException If the lookup doesn't have private access to the target class
         */
        MethodHandles.@NonNull Lookup privateLookupIn(@NonNull Class<?> targetClass, MethodHandles.@NonNull Lookup lookup) throws IllegalAccessException;

        /**
         * Defines a {@link Class} to the same {@link ClassLoader} and in the same runtime package
         * and {@link ProtectionDomain} as the {@link java.lang.invoke.MethodHandles.Lookup}'s lookup class.
         *
         * @param lookup The lookup of which the target class will be used to define the class in
         * @param byteCode The byte code of the class to define
         * @return The defined class
         * @throws IllegalAccessException If the lookup doesn't have package private access to the target package
         */
        @NonNull Class<?> defineClass(MethodHandles.@NonNull Lookup lookup, @NonNull byte[] byteCode) throws IllegalAccessException;
    }

    /**
     * Checks whether Java 9 or newer is available.
     *
     * @return Whether Java 9 or newer is available
     */
    private static boolean isJava9Available() {
        return findPrivateLookupMethodHandle() != null;
    }

    /**
     * Searches for the {@code privateLookupIn} method. Which is available since Java 9.
     *
     * @return The method handle of the private lookup method
     */
    private static @Nullable MethodHandle findPrivateLookupMethodHandle() {
        try {
            //noinspection JavaLangInvokeHandleSignature
            return MethodHandles.publicLookup().findVirtual(MethodHandles.class, "privateLookupIn",
                    MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * The implementation for Java 9 or newer.
     */
    private static final class Java9Adapter implements Adapter {

        private static final @NonNull MethodHandle privateLookupMethodHandle = requireNonNull(findPrivateLookupMethodHandle());
        private static final @NonNull MethodHandle defineClassMethodHandle = getDefineClassMethodHandle();

        /**
         * Gets the {@code defineClass} method. Which is available since Java 9.
         *
         * @return The method handle of the define class method
         */
        private static @Nullable MethodHandle getDefineClassMethodHandle() {
            //noinspection JavaLangInvokeHandleSignature
            return doUnchecked(() -> MethodHandles.publicLookup().findVirtual(MethodHandles.Lookup.class, "defineClass",
                    MethodType.methodType(Class.class, byte[].class)));
        }

        @Override
        public MethodHandles.@NonNull Lookup privateLookupIn(@NonNull Class<?> targetClass, MethodHandles.@NonNull Lookup lookup) {
            return doUnchecked(() -> (MethodHandles.Lookup) privateLookupMethodHandle.invoke(targetClass, lookup));
        }

        @Override
        public @NonNull Class<?> defineClass(MethodHandles.@NonNull Lookup lookup, @NonNull byte[] byteCode) {
            return doUnchecked(() -> (Class<?>) defineClassMethodHandle.invoke(lookup, byteCode));
        }
    }

    /**
     * The implementation for Java 8.
     */
    private static final class Java8Adapter implements Adapter {

        private static final MethodHandles.@NonNull Lookup trustedLookup = loadTrustedLookup();
        private static final @NonNull MethodHandle defineClassMethodHandle = getClassLoaderDefineMethodHandle();

        /**
         * Gets the {@link MethodHandle} for the {@code classloader.defineClass(...)} method.
         *
         * @return The method handle
         */
        private static @NonNull MethodHandle getClassLoaderDefineMethodHandle() {
            return doUnchecked(() -> trustedLookup.findVirtual(ClassLoader.class, "defineClass",
                    MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class)));
        }

        /**
         * Loads or constructs a trusted {@link MethodHandles} lookup.
         *
         * @return The trusted lookup
         */
        private static MethodHandles.@NonNull Lookup loadTrustedLookup() {
            try {
                // See if we can find the trusted lookup field directly
                final Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                field.setAccessible(true);
                return (MethodHandles.Lookup) field.get(null);
            } catch (NoSuchFieldException e) {
                // Not so much luck, let's try to hack something together another way
                // Get a public lookup and create a new instance
                final MethodHandles.Lookup lookup = MethodHandles.publicLookup().in(Object.class);

                try {
                    final Field field = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
                    field.setAccessible(true);

                    final Field mField = Field.class.getDeclaredField("modifiers");
                    mField.setAccessible(true);
                    mField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                    // The field that holds the trusted access mode
                    final Field trustedAccessModeField = MethodHandles.Lookup.class.getDeclaredField("TRUSTED");
                    trustedAccessModeField.setAccessible(true);

                    // Apply the modifier to the lookup instance
                    field.set(lookup, trustedAccessModeField.get(null));
                } catch (Exception e1) {
                    throw new IllegalStateException("Unable to create a trusted method handles lookup", e1);
                }

                return lookup;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to create a trusted method handles lookup", e);
            }
        }

        @Override
        public MethodHandles.@NonNull Lookup privateLookupIn(@NonNull Class<?> targetClass, MethodHandles.@NonNull Lookup lookup) {
            final SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
            return trustedLookup.in(targetClass);
        }

        @Override
        public @NonNull Class<?> defineClass(MethodHandles.@NonNull Lookup lookup, @NonNull byte[] byteCode) {
            final SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(new ReflectPermission("defineClass"));
            }
            if ((lookup.lookupModes() & MethodHandles.Lookup.PACKAGE) == 0) {
                throw throwUnchecked(new IllegalAccessException("Lookup does not have PACKAGE access"));
            }
            final String className;
            try {
                final ClassReader classReader = new ClassReader(byteCode);
                className = classReader.getClassName().replace('/', '.');
            } catch (RuntimeException e) {
                final ClassFormatError classFormatError = new ClassFormatError();
                classFormatError.initCause(e);
                throw classFormatError;
            }
            final Class<?> lookupClass = lookup.lookupClass();
            final String packageName = getPackageName(className);
            final String lookupPackageName = getPackageName(lookupClass);
            if (!packageName.equals(lookupPackageName)) {
                throw new IllegalArgumentException("Class not in same package as lookup class");
            }
            return AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
                final ClassLoader classLoader = lookupClass.getClassLoader();
                final ProtectionDomain protectionDomain = lookupClass.getProtectionDomain();
                return doUnchecked(() -> (Class<?>) defineClassMethodHandle.invoke(classLoader, className,
                        byteCode, 0, byteCode.length, protectionDomain));
            });
        }
    }
}
