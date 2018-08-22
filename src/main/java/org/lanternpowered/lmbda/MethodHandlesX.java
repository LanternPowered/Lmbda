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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A utility class full of magic related to {@link MethodHandles}.
 */
@SuppressWarnings("ThrowableNotThrown")
public final class MethodHandlesX {

    private static final MethodHandles.Lookup trustedLookup = loadTrustedLookup();
    private static final ProtectionDomainClassDefineSupport protectionDomainClassDefineSupport = loadProtectionDomainClassDefineSupport();

    /**
     * Gets a trusted {@link MethodHandles.Lookup} instance.
     *
     * @return The trusted method handles lookup
     * @throws SecurityException If the caller isn't allowed to access to trusted method handles lookup
     */
    public static MethodHandles.Lookup trustedLookup() {
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new ReflectPermission("trustedMethodHandlesLookup"));
        }
        return trustedLookup;
    }

    /**
     * Defines a class the same protection domain of the {@link MethodHandles.Lookup} target (package private access).
     *
     * @param lookup The lookup of which the target class will be used to define the class in
     * @param byteCode The byte code of the class to define
     * @return The defined class
     * @throws IllegalAccessException If the lookup doesn't have access in its target class
     */
    public static Class<?> defineClass(MethodHandles.Lookup lookup, byte[] byteCode) {
        return protectionDomainClassDefineSupport.defineClass(lookup, byteCode);
    }

    /**
     * Loads the trusted {@link MethodHandles.Lookup} instance.
     *
     * @return The trusted method handles lookup
     */
    private static MethodHandles.Lookup loadTrustedLookup() {
        return AccessController.doPrivileged((PrivilegedAction<MethodHandles.Lookup>) () -> {
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
                    // Make the field accessible
                    FieldAccessor.makeAccessible(field);

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
        });
    }

    private interface ProtectionDomainClassDefineSupport {

        Class<?> defineClass(MethodHandles.Lookup lookup, byte[] byteCode);
    }

    private static ProtectionDomainClassDefineSupport loadProtectionDomainClassDefineSupport() {
        return AccessController.doPrivileged((PrivilegedAction<ProtectionDomainClassDefineSupport>) () -> {
            try {
                final MethodHandle methodHandle = MethodHandles.publicLookup().findVirtual(
                        MethodHandles.Lookup.class, "defineClass", MethodType.methodType(Class.class, byte[].class));
                return (ProtectionDomainClassDefineSupport) (lookup, byteCode) -> {
                    try {
                        return (Class<?>) methodHandle.invoke(lookup, byteCode);
                    } catch (Throwable t) {
                        throw throwUnchecked(t);
                    }
                };
            } catch (NoSuchMethodException e) {
                // Not found, most likely not java 9
                try {
                    final MethodHandle methodHandle = trustedLookup.findVirtual(ClassLoader.class, "defineClass",
                            MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class));
                    return (ProtectionDomainClassDefineSupport) (lookup, byteCode) -> {
                        try {
                            return (Class<?>) methodHandle.invoke(lookup.lookupClass().getClassLoader(),
                                    null, byteCode, 0, byteCode.length);
                        } catch (Throwable t) {
                            throw throwUnchecked(t);
                        }
                    };
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            } catch (IllegalAccessException e) {
                throw throwUnchecked(e);
            }
        });
    }

    /**
     * Throws the {@link Throwable} as an unchecked exception.
     *
     * @param t The throwable to throw
     * @return A runtime exception
     */
    static RuntimeException throwUnchecked(Throwable t) {
        throwUnchecked0(t);
        throw new AssertionError("Unreachable.");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }

    private MethodHandlesX() {
    }
}
