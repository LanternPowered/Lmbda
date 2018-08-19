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
package org.lanternpowered.lmbda.fields;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.lanternpowered.lmbda.FieldAccess;
import org.lanternpowered.lmbda.UnsafeMethodHandles;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class FieldAccessorFactory {

    /**
     * A counter for all the generated getter classes.
     */
    private static final AtomicInteger getterCounter = new AtomicInteger();

    /**
     * A counter for all the generated setter classes.
     */
    private static final AtomicInteger setterCounter = new AtomicInteger();

    /**
     * The setter method type.
     */
    private static final MethodType setterMethodType = MethodType.methodType(void.class, Object.class, Object.class);

    /**
     * Creates a setter {@link BiConsumer} for the given {@link Field}.
     *
     * @param field The field
     * @param <T> The target object type
     * @param <V> The field value type
     * @return The bi consumer
     */
    public static <T, V> BiConsumer<T, V> createSetter(Field field) {
        requireNonNull(field, "field");
        field.setAccessible(true);

        boolean isFinal = Modifier.isFinal(field.getModifiers());
        // Better check is somebody changed the final modifier already
        if (!isFinal) {
            final Field[] fields = field.getDeclaringClass().getDeclaredFields();
            boolean isFound = false;
            for (Field field1 : fields) {
                // The same signature, now check if somebody tinkered with the field
                if (field.getName().equals(field1.getName()) &&
                        field.getType().equals(field1.getType())) {
                    isFinal = Modifier.isFinal(field1.getModifiers());
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                throw new IllegalStateException("Something funky happened with: " + field.getName());
            }
        } else {
            FieldAccess.makeAccessible(field);
        }

        // Final fields don't allow direct access, so MethodHandles will do the trick.
        if (isFinal) {
            try {
                final MethodHandle methodHandle = UnsafeMethodHandles.trustedLookup()
                        .in(field.getDeclaringClass()).unreflectSetter(field).asType(setterMethodType);
                return (a, b) -> {
                    try {
                        methodHandle.invokeExact(a, b);
                    } catch (Throwable throwable) {
                        throw new IllegalStateException(throwable);
                    }
                };
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        final ClassWriter cw = new ClassWriter(0);
        final String className = field.getName().replace('.', '/') + "$$LmbdaSetter$" + setterCounter.incrementAndGet();
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className,
                "Ljava/lang/Object;Ljava/util/function/BiConsumer<Ljava/lang/Object;Ljava/lang/Object;>;",
                "java/lang/Object", new String[] { "java/util/function/BiConsumer" });

        // Add a empty constructor
        BytecodeUtils.visitEmptyConstructor(cw);
        // Generate the apply method
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "accept",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
        mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true);
        mv.visitCode();
        final String descriptor = Type.getDescriptor(field.getType());
        final String targetName = Type.getInternalName(field.getDeclaringClass());
        final boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (!isStatic) {
            // Load the target parameter
            mv.visitVarInsn(ALOAD, 1);
            // Cast it
            mv.visitTypeInsn(CHECKCAST, targetName);
        }
        // Load the value parameter
        mv.visitVarInsn(ALOAD, 2);
        // Unbox the values in case they are primitives, otherwise cast
        BytecodeUtils.visitConversion(mv, Object.class, field.getType());
        // Put the value into the field
        if (isStatic) {
            mv.visitFieldInsn(PUTSTATIC, targetName, field.getName(), descriptor);
        } else {
            mv.visitFieldInsn(PUTFIELD, targetName, field.getName(), descriptor);
        }
        // Return
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();

        // Finish class generation
        cw.visitEnd();

        // Define the class and create a function instance
        final MethodHandles.Lookup lookup = UnsafeMethodHandles.trustedLookup().in(field.getDeclaringClass());
        final Class<?> functionClass = UnsafeMethodHandles.defineNestmateClass(lookup, cw.toByteArray());

        try {
            return (BiConsumer<T, V>) functionClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Something went wrong!", e);
        }
    }

    private FieldAccessorFactory() {
    }
}
