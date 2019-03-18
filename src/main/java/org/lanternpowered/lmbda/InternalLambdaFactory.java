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
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Separated from {@link LambdaFactory} to keep it clean.
 */
final class InternalLambdaFactory {

    /**
     * The package name we will define custom classes in.
     */
    private static final String packageName;

    /**
     * The internal lookup that has access to this library package.
     */
    private static final MethodHandles.Lookup internalLookup = AccessController.doPrivileged(
            (PrivilegedAction<MethodHandles.Lookup>) MethodHandles::lookup);

    /**
     * A thread local which temporarily holds the {@link MethodHandle}
     * that will be injected into the generated class.
     */
    @SuppressWarnings("WeakerAccess") // Must be package private! Is accessed by generated classes.
    static final ThreadLocal<MethodHandle> currentMethodHandle = new ThreadLocal<>();

    /**
     * A counter to make sure that lambda names don't conflict.
     */
    private static final AtomicInteger lambdaCounter = new AtomicInteger();

    static {
        final String name = InternalLambdaFactory.class.getName();
        packageName = name.substring(0, name.lastIndexOf('.'));
    }

    @SuppressWarnings("unchecked")
    static <T, F extends T> F create(LambdaType<T> lambdaType, MethodHandle methodHandle) {
        requireNonNull(lambdaType, "lambdaType");
        requireNonNull(methodHandle, "methodHandle");

        try {
            return createGeneratedFunction(lambdaType, methodHandle);
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't create lambda for: \"" + methodHandle + "\". "
                    + "Failed to implement: " + lambdaType, e);
        }
    }

    private static String toGenericDescriptor(Class<?> superClass, ParameterizedType genericType) {
        final Map<String, TypeVariable<?>> typeVariables = new HashMap<>();

        final StringBuilder signatureBuilder = new StringBuilder();
        toGenericSignature(signatureBuilder, genericType, typeVariables);

        final StringBuilder descriptorBuilder = new StringBuilder();

        if (!typeVariables.isEmpty()) {
            descriptorBuilder.append('<');
            for (TypeVariable typeVariable : typeVariables.values()) {
                descriptorBuilder.append(typeVariable.getName());
                for (java.lang.reflect.Type bound : typeVariable.getBounds()) {
                    final boolean isFinal;
                    if (bound instanceof Class) {
                        isFinal = Modifier.isFinal(((Class) bound).getModifiers());
                    } else if (bound instanceof GenericArrayType) {
                        throw new IllegalStateException(); // Should never happen
                    } else if (bound instanceof ParameterizedType) {
                        isFinal = Modifier.isFinal(((Class) ((ParameterizedType) bound).getRawType()).getModifiers());
                    } else {
                        isFinal = false;
                    }
                    if (isFinal) {
                        descriptorBuilder.append(':');
                    }
                    descriptorBuilder.append(':');
                    toGenericSignature(descriptorBuilder, bound, null);
                }
            }
            descriptorBuilder.append('>');
        }

        descriptorBuilder.append(Type.getDescriptor(superClass));
        descriptorBuilder.append(signatureBuilder.toString());

        return descriptorBuilder.toString();
    }

    private static void toGenericSignature(StringBuilder builder, java.lang.reflect.Type type, @Nullable Map<String, TypeVariable<?>> typeVariables) {
        if (type instanceof Class) {
            builder.append(Type.getDescriptor((Class) type));
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType arrayType = (GenericArrayType) type;
            builder.append('[');
            toGenericSignature(builder, arrayType.getGenericComponentType(), typeVariables);
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            builder.append('L');
            builder.append(Type.getInternalName((Class) parameterizedType.getRawType()));
            builder.append('<');
            for (java.lang.reflect.Type parameter : parameterizedType.getActualTypeArguments()) {
                toGenericSignature(builder, parameter, typeVariables);
            }
            builder.append('>').append(';');
        } else if (type instanceof TypeVariable) {
            final TypeVariable typeVariable = (TypeVariable) type;
            builder.append('T').append(typeVariable.getName()).append(';');
            if (typeVariables != null) {
                typeVariables.put(typeVariable.getName(), typeVariable);
            }
        } else if (type instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) type;

            final java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();
            final java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();

            final boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
            final boolean hasUpper = upperBounds != null && upperBounds.length > 0;

            if (hasUpper && hasLower && Object.class.equals(lowerBounds[0]) && Object.class.equals(upperBounds[0])) {
                builder.append('*');
            } else if (hasLower) {
                builder.append('-');
                for (java.lang.reflect.Type lower : lowerBounds) {
                    toGenericSignature(builder, lower, typeVariables);
                }
            } else if (hasUpper) {
                if (upperBounds.length == 1 && Object.class.equals(upperBounds[0])) {
                    builder.append('*');
                } else {
                    builder.append('+');
                    for (java.lang.reflect.Type upper : upperBounds) {
                        toGenericSignature(builder, upper, typeVariables);
                    }
                }
            } else {
                builder.append('*');
            }
        }
    }

    private static final String METHOD_HANDLE_FIELD_NAME = "methodHandle";

    @SuppressWarnings("unchecked")
    private static <T> T createGeneratedFunction(LambdaType lambdaType, MethodHandle methodHandle) {
        // Convert the method handle types to match the functional method signature,
        // this will make sure that all the objects are converted accordingly so
        // we don't have to do it ourselves with asm.
        // This will also throw an exception if the functional interface cannot be
        // implemented by the given method handle
        methodHandle = methodHandle.asType(lambdaType.methodType);

        final Method method = lambdaType.getMethod();
        final ClassWriter cw = new ClassWriter(0);

        // The function class will be defined in the package of this library to have
        // access to package private fields, in java 9 this is also the package we
        // can use to define classes in
        final String className = packageName + ".Lmbda$" + lambdaCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        final Class<?> superClass = Object.class;
        String genericDescriptor = null;
        if (lambdaType.genericFunctionType != null) {
            genericDescriptor = toGenericDescriptor(superClass, lambdaType.genericFunctionType);
        }

        cw.visit(V1_8, ACC_SUPER, internalClassName, genericDescriptor, Type.getInternalName(superClass),
                new String[] { Type.getInternalName(lambdaType.getFunctionClass()) });

        final FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
                METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;", null, null);
        fv.visitEnd();

        // Add a package private constructor
        MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Add the method handle field
        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, Type.getInternalName(InternalLambdaFactory.class),
                "currentMethodHandle", "Ljava/lang/ThreadLocal;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "java/lang/invoke/MethodHandle");
        mv.visitFieldInsn(PUTSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        // Write the function method
        final String descriptor = Type.getMethodDescriptor(method);
        mv = cw.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        // Hide the lambda from the stack trace
        mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true).visitEnd();
        mv.visitCode();
        // Start body
        mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
        // Load all the parameters
        final Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            visitLoad(mv, Type.getType(parameters[i]), 1 + i);
        }
        // Invoke the method
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", descriptor, false);
        // Return
        visitReturn(mv, Type.getType(method.getReturnType()));
        // End body
        final int maxs = parameters.length + 1;
        mv.visitMaxs(maxs, maxs);
        mv.visitEnd();

        cw.visitEnd();

        try {
            // Store the current method handle, it will be
            // required on initialization of the generated class
            currentMethodHandle.set(methodHandle);

            // Define the class within this package
            final Class<?> theClass = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> MethodHandlesX.doUnchecked(
                    () -> MethodHandlesX.defineClass(internalLookup, cw.toByteArray())));

            // Instantiate the function object
            return MethodHandlesX.doUnchecked(
                    () -> (T) internalLookup.in(theClass).findConstructor(theClass, MethodType.methodType(void.class)).invoke());
        } finally {
            // Cleanup
            currentMethodHandle.remove();
        }
    }

    /**
     * Visits the {@link MethodVisitor} to apply the load
     * operation for the given return {@link Type}
     * and parameter index.
     *
     * @param mv The method visitor
     * @param type The return type
     */
    private static void visitLoad(MethodVisitor mv, Type type, int parameter) {
        final int sort = type.getSort();
        if (sort == Type.BYTE ||
                sort == Type.BOOLEAN ||
                sort == Type.SHORT ||
                sort == Type.CHAR ||
                sort == Type.INT) {
            mv.visitVarInsn(ILOAD, parameter);
        } else if (sort == Type.DOUBLE) {
            mv.visitVarInsn(DLOAD, parameter);
        } else if (sort == Type.FLOAT) {
            mv.visitVarInsn(FLOAD, parameter);
        } else if (sort == Type.LONG) {
            mv.visitVarInsn(LLOAD, parameter);
        } else if (sort == Type.VOID) {
            throw new IllegalStateException("Cannot load void parameter");
        } else {
            mv.visitVarInsn(ALOAD, parameter);
        }
    }

    /**
     * Visits the {@link MethodVisitor} to apply the return
     * operation for the given return {@link Type}.
     *
     * @param mv The method visitor
     * @param type The return type
     */
    private static void visitReturn(MethodVisitor mv, Type type) {
        final int sort = type.getSort();
        if (sort == Type.BYTE ||
                sort == Type.BOOLEAN ||
                sort == Type.SHORT ||
                sort == Type.CHAR ||
                sort == Type.INT) {
            mv.visitInsn(IRETURN);
        } else if (sort == Type.DOUBLE) {
            mv.visitInsn(DRETURN);
        } else if (sort == Type.FLOAT) {
            mv.visitInsn(FRETURN);
        } else if (sort == Type.LONG) {
            mv.visitInsn(LRETURN);
        } else if (sort == Type.VOID) {
            mv.visitInsn(RETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
    }
}
