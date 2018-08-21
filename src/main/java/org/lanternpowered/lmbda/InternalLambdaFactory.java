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
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
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

                    // Whether method handle reflection should be used,
                    // reflectAs cannot be used when the field is final
                    // for a setter method handle and will throw an
                    // exception, so we have to get the
                    // field a other way in this case
                    boolean reflect = false;
                    if ((refKind == MethodHandleInfo.REF_putField || refKind == MethodHandleInfo.REF_putStatic)
                            && Modifier.isFinal(info.getModifiers())) {
                        // Final fields cannot be accessed directly, so
                        // fall back to method handles in this case.
                        reflect = true;
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

                    // Whether nestmates are available
                    final boolean nestmates = MethodHandlesX.isNestmateClassDefiningSupported();

                    // If it's a private field and nestmate classes aren't available
                    // then we also need to fallback to method handle reflection,
                    // other access modifiers can still be accessed by injecting
                    // the class in the same package.
                    if (Modifier.isPrivate(field.getModifiers()) && !nestmates) {
                        reflect = true;
                    }

                    final Class<?> theClass;
                    if (reflect) {
                        byte[] bytecode = null;

                        switch (info.getReferenceKind()) {
                            case MethodHandleInfo.REF_getField:
                                bytecode = createMethodHandleFieldGetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_getStatic:
                                bytecode = createMethodHandleStaticFieldGetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_putField:
                                bytecode = createMethodHandleFieldSetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_putStatic:
                                bytecode = createMethodHandleStaticFieldSetLambda(functionalInterface, field);
                                break;
                        }

                        requireNonNull(bytecode);

                        // Reflect is always injected into the lmbda package to access internal fields
                        theClass = MethodHandlesX.defineClass(
                                MethodHandlesX.trustedLookup.in(MethodHandlesX.class), bytecode);
                    } else {
                        byte[] bytecode = null;

                        switch (info.getReferenceKind()) {
                            case MethodHandleInfo.REF_getField:
                                bytecode = createFieldGetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_getStatic:
                                bytecode = createStaticFieldGetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_putField:
                                bytecode = createFieldSetLambda(functionalInterface, field);
                                break;
                            case MethodHandleInfo.REF_putStatic:
                                bytecode = createStaticFieldSetLambda(functionalInterface, field);
                                break;
                        }

                        requireNonNull(bytecode);

                        if (nestmates) {
                            theClass = MethodHandlesX.defineNestmateClass(lookup, bytecode);
                        } else {
                            theClass = MethodHandlesX.defineClass(lookup, bytecode);
                        }
                    }

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

    private static byte[] createFieldGetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("The functional interface requires exactly one parameter, "
                    + "the target object to retrieve the value from.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> parameterTargetType = method.getParameterTypes()[0];
        final Class<?> returnType = method.getReturnType();
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitFunctionMethod(cw, method, mv -> {
            // Load the target object
            mv.visitVarInsn(ALOAD, 1);
            // Convert the target object
            BytecodeUtils.visitConversion(mv, parameterTargetType, field.getDeclaringClass());
            // Put the value in the field
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(fieldType));
            // Convert the field value so that we can return it
            BytecodeUtils.visitConversion(mv, fieldType, returnType);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createStaticFieldGetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException("The functional interface requires zero parameters.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> returnType = method.getReturnType();
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitFunctionMethod(cw, method, mv -> {
            // Get the value from the field
            mv.visitFieldInsn(GETSTATIC, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(fieldType));
            // Convert the field value so that we can return it
            BytecodeUtils.visitConversion(mv, fieldType, returnType);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createFieldSetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 2) {
            throw new IllegalArgumentException("The functional interface requires exactly two parameters, "
                    + "the target object and value to put in the field.");
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of the functional interface must be void.");
        }

        final Class<?> parameterTargetType = method.getParameterTypes()[0];
        final Class<?> parameterType = method.getParameterTypes()[1];
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitFunctionMethod(cw, method, mv -> {
            mv.visitVarInsn(ALOAD, 1);
            // Convert the target object
            BytecodeUtils.visitConversion(mv, parameterTargetType, field.getDeclaringClass());
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(parameterType), 2);
            // Convert the parameter value so that we can put it in the field
            BytecodeUtils.visitConversion(mv, parameterType, fieldType);
            // Put the value in the field
            mv.visitFieldInsn(PUTFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(fieldType));
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createStaticFieldSetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        validateStaticFieldSetLambdaMethod(method);

        final Class<?> parameterType = method.getParameterTypes()[0];
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitFunctionMethod(cw, method, mv -> {
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(parameterType), 1);
            // Convert the parameter value so that we can put it in the field
            BytecodeUtils.visitConversion(mv, parameterType, fieldType);
            // Put the value in the field
            mv.visitFieldInsn(PUTSTATIC, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(fieldType));
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static final String METHOD_HANDLE_FIELD_NAME = "methodHandle";

    private static byte[] createMethodHandleFieldGetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("The functional interface requires exactly one parameter, "
                    + "the target object to retrieve the value from.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> parameterTargetType = method.getParameterTypes()[0];
        final Class<?> returnType = method.getReturnType();
        final Class<?> targetType = field.getDeclaringClass();
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitMethodHandleField(cw, internalClassName, "findGetter", field, field.getType(), targetType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the target object
            mv.visitVarInsn(ALOAD, 1);
            // Convert the target object
            BytecodeUtils.visitConversion(mv, parameterTargetType, targetType);
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(targetType) + ")" + Type.getDescriptor(fieldType), false);
            // Convert the field value so that we can return it
            BytecodeUtils.visitConversion(mv, fieldType, returnType);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createMethodHandleStaticFieldGetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException("The functional interface requires zero parameters.");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("The return type of the functional interface may not be void.");
        }

        final Class<?> returnType = method.getReturnType();
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$get$" + field.getName() + "$" + getterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitMethodHandleField(cw, internalClassName, "findStaticSetter", field, void.class, fieldType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "()" + Type.getDescriptor(fieldType), false);
            // Convert the field value so that we can return it
            BytecodeUtils.visitConversion(mv, fieldType, returnType);
            // Return the value
            BytecodeUtils.visitReturn(mv, Type.getType(returnType));
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createMethodHandleFieldSetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        if (method.getParameterCount() != 2) {
            throw new IllegalArgumentException("The functional interface requires exactly two parameters, "
                    + "the target object and value to put in the field.");
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of the functional interface must be void.");
        }

        final Class<?> parameterTargetType = method.getParameterTypes()[0];
        final Class<?> parameterType = method.getParameterTypes()[1];
        final Class<?> targetType = field.getDeclaringClass();
        final Class<?> fieldType = field.getType();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitMethodHandleField(cw, internalClassName, "findSetter", field, void.class, targetType, fieldType);
        visitFunctionMethod(cw, method, mv -> {
            // Load the method handle to invoke
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the target object
            BytecodeUtils.visitLoad(mv, Type.getType(parameterType), 1);
            // Convert the target object
            BytecodeUtils.visitConversion(mv, parameterTargetType, targetType);
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(parameterType), 2);
            // Convert the parameter value so that we can put it in the field
            BytecodeUtils.visitConversion(mv, parameterType, fieldType);
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(targetType) + Type.getDescriptor(fieldType) + ")V", false);
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] createMethodHandleStaticFieldSetLambda(FunctionalInterface functionalInterface, Field field) {
        final Method method = functionalInterface.getMethod();
        validateStaticFieldSetLambdaMethod(method);

        final Class<?> parameterType = method.getParameterTypes()[0];
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final String className = field.getDeclaringClass().getName() + "$$Lmbda$set$" + field.getName() + "$" + setterCounter.incrementAndGet();
        final String internalClassName = className.replace('.', '/');

        cw.visit(V1_8, ACC_SUPER, internalClassName, null, "java/lang/Object",
                new String[] { Type.getInternalName(functionalInterface.getFunctionClass())});

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(ACC_PRIVATE, cw);

        visitMethodHandleField(cw, internalClassName, "findStaticSetter", field, void.class, field.getType());
        visitFunctionMethod(cw, method, mv -> {
            mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;");
            // Load the value that will be put in the static field
            BytecodeUtils.visitLoad(mv, Type.getType(parameterType), 1);
            // Convert the parameter value so that we can put it in the method handle invocation
            BytecodeUtils.visitConversion(mv, parameterType, field.getType());
            // Invoke the method handle, the "invokeExact" method is polymorphic
            // so the descriptor will be different depending on the field type
            // This must match the signature provided by the constructed method handle
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    "(" + Type.getDescriptor(field.getType()) + ")V", false);
            // Just return
            mv.visitInsn(RETURN);
        });

        cw.visitEnd();
        return cw.toByteArray();
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
        mv.visitLdcInsn(Type.getType(field.getType()));
        // Find the static setter
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", functionType,
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);
        // Call asType to remove any generics from the setter method handle
        if (returnType == void.class) {
            mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
        } else {
            mv.visitLdcInsn(Type.getType(returnType));
        }
        // Create parameters array
        BytecodeUtils.visitPushInt(mv, parameterTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        for (int i = 0; i < parameterTypes.length; i++) {
            mv.visitInsn(DUP);
            BytecodeUtils.visitPushInt(mv, i);
            mv.visitLdcInsn(Type.getType(parameterTypes[i]));
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

    private static void validateStaticFieldSetLambdaMethod(Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("The functional interface requires exactly one parameter, "
                    + "the value that will be put in the static field.");
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of the functional interface must be void.");
        }
    }
}
