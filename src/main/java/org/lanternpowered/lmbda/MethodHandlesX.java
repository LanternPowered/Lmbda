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

import static org.objectweb.asm.Opcodes.ASM5;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A utility class full of magic related to {@link MethodHandles}.
 */
@SuppressWarnings("ThrowableNotThrown")
public final class MethodHandlesX {

    static final MethodHandles.Lookup trustedLookup = loadTrustedLookup();
    private static final NestmateClassDefineSupport nestmateClassDefineSupport = loadNestmateClassDefineSupport();
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
     * Whether the feature of defining nestmate classes through
     * {@link #defineNestmateClass(MethodHandles.Lookup, byte[])} is available.
     *
     * @return Whether nestmate classes support is available
     */
    public static boolean isNestmateClassDefiningSupported() {
        return nestmateClassDefineSupport != null;
    }

    /**
     * Defines a "nest mate" class on the {@link MethodHandles.Lookup} target.
     *
     * <p>Nestmate classes have private access for fields/methods/constructors
     * on the target class. This allows you to access them without the need
     * of reflection.</p>
     *
     * <p>The provided {@link MethodHandles.Lookup} must have private access
     * in order to be able to define a nestmate class.
     * See {@link MethodHandles.Lookup#PRIVATE}</p>
     *
     * @param lookup The lookup of which the target class will be used to define the class in
     * @param byteCode The byte code of the class to define
     * @return The defined class
     * @throws IllegalAccessException If the lookup doesn't have private access in its target class
     */
    public static Class<?> defineNestmateClass(MethodHandles.Lookup lookup, byte[] byteCode) {
        if (nestmateClassDefineSupport == null) {
            throw new UnsupportedOperationException("Nestmate class defining isn't available.");
        }
        if ((lookup.lookupModes() & MethodHandles.Lookup.PRIVATE) == 0) {
            throw throwUnchecked(new IllegalAccessException(
                    "The lookup doesn't have private access, which is required to define nestmate classes."));
        }
        return nestmateClassDefineSupport.defineNestmate(lookup, byteCode);
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

    private final static class StopVisiting extends RuntimeException {

        final static StopVisiting INSTANCE = new StopVisiting(); // Internal access only

        private StopVisiting() {
        }

        @Override
        public Throwable fillInStackTrace() {
            setStackTrace(new StackTraceElement[0]);
            return this;
        }
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
                        // Search for the class name
                        final ClassReader reader = new ClassReader(byteCode);
                        final String[] theName = new String[1];
                        try {
                            reader.accept(new ClassVisitor(ASM5) {
                                @Override
                                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                    theName[0] = name;
                                    throw StopVisiting.INSTANCE;
                                }
                            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        } catch (StopVisiting ignored) {
                        }
                        try {
                            return (Class<?>) methodHandle.invoke(lookup.lookupClass().getClassLoader(),
                                    theName[0].replace('/', '.'), byteCode, 0, byteCode.length);
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
     * A internal access for defining "nest mate" classes.
     */
    private interface NestmateClassDefineSupport {

        Class<?> defineNestmate(MethodHandles.Lookup lookup, byte[] byteCode);
    }

    /**
     * Loads the {@link NestmateClassDefineSupport} instance.
     *
     * @return The method handles lookup access
     */
    private static NestmateClassDefineSupport loadNestmateClassDefineSupport() {
        return AccessController.doPrivileged((PrivilegedAction<NestmateClassDefineSupport>) () -> {
            // Currently, only Unsafe provides access to defining nestmate classes,
            // this will most likely change in the future:
            // JDK-8171335
            // JDK-8172672
            // TODO: Keep this up to date
            Class<?> unsafeClass;
            try {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            } catch (ClassNotFoundException e) {
                try {
                    unsafeClass = Class.forName("jdk.unsupported.misc.Unsafe");
                } catch (ClassNotFoundException ex) {
                    // Print the error message
                    new IllegalStateException("Unable to find the Unsafe class.", e).printStackTrace();
                    // Just return null, nestmate classes aren't supported :(
                    return null;
                }
            }
            try {
                final Field field = unsafeClass.getDeclaredField("theUnsafe");
                field.setAccessible(true);

                // Get the defineAnonymousClass method
                final Method method = unsafeClass.getDeclaredMethod("defineAnonymousClass",
                        Class.class, byte[].class, Object[].class);
                method.setAccessible(true);
                // Get the method handle for the method
                final MethodHandle methodHandle = trustedLookup.in(unsafeClass).unreflect(method);

                // Get the Unsafe instance
                final Object unsafe = field.get(null);

                // Create the lookup access
                return (NestmateClassDefineSupport) (lookup, bytes) -> {
                    try {
                        return (Class<?>) methodHandle.invoke(unsafe, lookup.lookupClass(), bytes, null);
                    } catch (Throwable throwable) {
                        throw new IllegalStateException(throwable);
                    }
                };
            } catch (Exception e) {
                throw new IllegalStateException("Unable to access the Unsafe instance.", e);
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
