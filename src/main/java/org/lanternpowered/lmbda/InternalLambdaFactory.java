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
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Separated from {@link LambdaFactory} to keep it clean.
 */
final class InternalLambdaFactory {

    private static final AtomicInteger setterCounter = new AtomicInteger();
    private static final AtomicInteger getterCounter = new AtomicInteger();

    static <T, F extends T> F create(FunctionalInterface<T> functionalInterface, Executable executable) {
        requireNonNull(functionalInterface, "functionalInterface");
        requireNonNull(executable, "executable");

        try {
            // The trusted lookup can be used here, because the privileges are already
            // required to make the executable accessible. This unreflect methods will
            // fail if this isn't the case.

            final MethodHandles.Lookup lookup = MethodHandlesX.trustedLookup.in(executable.getDeclaringClass());
            final MethodHandle methodHandle;
            if (executable instanceof Constructor) {
                methodHandle = lookup.unreflectConstructor((Constructor<?>) executable);
            } else {
                methodHandle = lookup.unreflect((Method) executable);
            }

            return create(functionalInterface, methodHandle);
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't create lambda for: \"" + executable + "\". "
                    + "Failed to implement: " + functionalInterface, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T, F extends T> F create(FunctionalInterface<T> functionalInterface, MethodHandle methodHandle) {
        requireNonNull(functionalInterface, "functionalInterface");
        requireNonNull(methodHandle, "methodHandle");

        try {
            MethodHandles.Lookup lookup = MethodHandlesX.trustedLookup;
            final MethodHandleInfo info = lookup.revealDirect(methodHandle);
            lookup = lookup.in(info.getDeclaringClass());

            // Add support for generating lambda functions for fields
            final int refKind = info.getReferenceKind();
            switch (refKind) {
                case MethodHandleInfo.REF_getField:
                case MethodHandleInfo.REF_getStatic:
                case MethodHandleInfo.REF_putField:
                case MethodHandleInfo.REF_putStatic:
                    final Class<?> declaringClass = info.getDeclaringClass();
                    final Field field;

                    if ((refKind == MethodHandleInfo.REF_putField || refKind == MethodHandleInfo.REF_putStatic)
                            && Modifier.isFinal(info.getModifiers())) {
                        // reflectAs throws an exception for final fields
                        field = AccessController.doPrivileged((PrivilegedAction<Field>) () ->
                                Arrays.stream(declaringClass.getDeclaredFields())
                                        .filter(field1 -> field1.getName().equals(info.getName()) &&
                                                field1.getType().equals(info.getMethodType().parameterType(0)))
                                        .findFirst().orElseThrow(() -> new IllegalStateException("Field not field: " + info))
                        );
                    } else {
                        field = info.reflectAs(Field.class, lookup);
                    }

                    ClassData classData = null;

                    switch (info.getReferenceKind()) {
                        case MethodHandleInfo.REF_getField:
                            classData = createFieldGetterLambda(functionalInterface, field);
                            break;
                        case MethodHandleInfo.REF_getStatic:
                            classData = createStaticFieldGetterLambda(functionalInterface, field);
                            break;
                        case MethodHandleInfo.REF_putField:
                            classData = createFieldSetterLambda(functionalInterface, field);
                            break;
                        case MethodHandleInfo.REF_putStatic:
                            classData = createStaticFieldSetterLambda(functionalInterface, field);
                            break;
                    }

                    requireNonNull(classData);

                    // Inject the class into the same class loader as the lambda factory
                    final Class<?> theClass = MethodHandlesX.defineClass(
                            MethodHandlesX.trustedLookup.in(MethodHandlesX.class), classData.name, classData.bytecode);

                    return AccessController.doPrivileged((PrivilegedAction<F>) () -> {
                        try {
                            final Constructor<?> constructor = theClass.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            return (F) constructor.newInstance();
                        } catch (Throwable e) {
                            throw MethodHandlesX.throwUnchecked(e);
                        }
                    });
            }

            // Generate the lambda class
            final CallSite callSite = LambdaMetafactory.metafactory(lookup, functionalInterface.getMethod().getName(),
                    functionalInterface.classType, functionalInterface.methodType, methodHandle, methodHandle.type());

            // Create the function
            return (F) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't create lambda for: \"" + methodHandle + "\". "
                    + "Failed to implement: " + functionalInterface, e);
        }
    }

    private static final String METHOD_HANDLE_FIELD_NAME = "methodHandle";

    static final class ClassData {

        final byte[] bytecode;
        final String name;

        ClassData(byte[] bytecode, String name) {
            this.bytecode = bytecode;
            this.name = name;
        }
    }

    private static ClassData createFieldGetterLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("The functional interface requires exactly one parameter, "
                    + "the target object to retrieve the value from.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> targetType = method.getParameterTypes()[0];
        final Class<?> returnType = method.getReturnType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitPrivateEmptyConstructor(cw);

        visitMethodHandleField(cw, internalClassName, "findGetter", field, returnType, targetType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the target object
            BytecodeUtils.visitLoad(mv, Type.getType(targetType), 1);
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(targetType) + ")" + Type.getDescriptor(returnType), false);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return new ClassData(cw.toByteArray(), className);
    }

    private static ClassData createStaticFieldGetterLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException("The functional interface requires zero parameters.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> returnType = method.getReturnType();
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitPrivateEmptyConstructor(cw);

        visitMethodHandleField(cw, internalClassName, "findStaticGetter", field, returnType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "()" + Type.getDescriptor(returnType), false);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return new ClassData(cw.toByteArray(), className);
    }

    private static ClassData createFieldSetterLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 2) {
            throw new IllegalArgumentException("The functional interface requires exactly two parameters, "
                    + "the target object and value to put in the field.");
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of the functional interface must be void.");
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?> targetType = parameterTypes[0];
        final Class<?> valueType = parameterTypes[1];

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitPrivateEmptyConstructor(cw);

        visitMethodHandleField(cw, internalClassName, "findSetter", field, void.class, targetType, valueType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the target object
            BytecodeUtils.visitLoad(mv, Type.getType(targetType), 1);
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(valueType), 2);
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(targetType) + Type.getDescriptor(valueType) + ")V", false);
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return new ClassData(cw.toByteArray(), className);
    }

    private static ClassData createStaticFieldSetterLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("The functional interface requires exactly one parameter, "
                    + "the value that will be put in the static field.");
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of the functional interface must be void.");
        }

        final Class<?> valueType = method.getParameterTypes()[0];
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitPrivateEmptyConstructor(cw);

        visitMethodHandleField(cw, internalClassName, "findStaticSetter", field, void.class, valueType);
        visitFunctionMethod(cw, method, mv -> {
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(valueType), 1);
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(valueType) + ")V", false);
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return new ClassData(cw.toByteArray(), className);
    }

    private static void visitFunctionMethod(ClassVisitor cv, Method functionMethod, Consumer<MethodVisitor> consumer) {
        // Write the function method
        final MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, functionMethod.getName(),
                Type.getMethodDescriptor(functionMethod), null, null);
        // Hide the lambda from the stack trace
        mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true);
        // Start method body
        mv.visitCode();
        // Apply body code
        consumer.accept(mv);
        // Auto compute maximum stack size
        mv.visitMaxs(0, 0);
        // End
        mv.visitEnd();
    }

    private static void visitMethodHandleField(ClassVisitor cv, String internalClassName, String functionType, Field field,
            Class<?> returnType, Class<?>... parameterTypes) {
        final FieldVisitor fv = cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
                METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;", null, null);
        fv.visitEnd();

        final MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PRIVATE, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, Type.getInternalName(MethodHandlesX.class),
                "trustedLookup", "Ljava/lang/invoke/MethodHandles$Lookup;");
        // Target class to find the field
        mv.visitLdcInsn(Type.getType(field.getDeclaringClass()));
        // Field name
        mv.visitLdcInsn(field.getName());
        // Field type
        BytecodeUtils.visitLoadType(mv, Type.getType(field.getType()));
        // Find the static setter
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", functionType,
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);
        // Call asType to remove any generics from the setter method handle
        BytecodeUtils.visitLoadType(mv, Type.getType(returnType));
        // Create parameters array
        BytecodeUtils.visitPushInt(mv, parameterTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        for (int i = 0; i < parameterTypes.length; i++) {
            mv.visitInsn(DUP);
            BytecodeUtils.visitPushInt(mv, i);
            BytecodeUtils.visitLoadType(mv, Type.getType(parameterTypes[i]));
            mv.visitInsn(AASTORE);
        }
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType",
                "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType",
                "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        // Store the constructed method handle
        mv.visitFieldInsn(PUTSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
