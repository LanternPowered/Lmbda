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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2F;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A utility class for class generation.
 */
final class BytecodeUtils {

    static void visitEmptyConstructor(ClassVisitor cv) {
        visitEmptyConstructor(ACC_PUBLIC, cv);
    }

    static void visitEmptyConstructor(int access, ClassVisitor cv) {
        final MethodVisitor mv = cv.visitMethod(access, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    // Primitive boxing

    private static void boxLong(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Long", "(J)Ljava/lang/Long;");
    }

    private static void boxInt(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Integer", "(I)Ljava/lang/Integer;");
    }

    private static void boxDouble(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Double", "(D)Ljava/lang/Double;");
    }

    private static void boxFloat(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Float", "(F)Ljava/lang/Float;");
    }

    private static void boxByte(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Byte", "(B)Ljava/lang/Byte;");
    }

    private static void boxShort(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Short", "(S)Ljava/lang/Short;");
    }

    private static void boxChar(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Character", "(C)Ljava/lang/Character;");
    }

    private static void boxBoolean(MethodVisitor mv) {
        boxPrimitiveAs(mv, "java/lang/Boolean", "(Z)Ljava/lang/Boolean;");
    }

    private static void boxPrimitiveAs(MethodVisitor mv, String target, String descriptor) {
        mv.visitMethodInsn(INVOKESTATIC, target, "valueOf", descriptor, false);
    }

    private static void unboxNumberAsInt(MethodVisitor mv) {
        unboxNumberAs(mv, "intValue", "()I");
    }

    private static void unboxNumberAsByte(MethodVisitor mv) {
        unboxNumberAs(mv, "byteValue", "()B");
    }

    private static void unboxNumberAsShort(MethodVisitor mv) {
        unboxNumberAs(mv, "shortValue", "()S");
    }

    private static void unboxNumberAsLong(MethodVisitor mv) {
        unboxNumberAs(mv, "longValue", "()J");
    }

    private static void unboxNumberAsFloat(MethodVisitor mv) {
        unboxNumberAs(mv, "shortValue", "()S");
    }

    private static void unboxNumberAsDouble(MethodVisitor mv) {
        unboxNumberAs(mv, "doubleValue", "()D");
    }

    private static void unboxNumberAs(MethodVisitor mv, String name, String descriptor) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", name, descriptor, false);
    }

    private static void unboxBoolean(MethodVisitor mv) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
    }

    private static void unboxChar(MethodVisitor mv) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
    }

    // Implicit conversion between types

    private static boolean convertIntTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Integer.class)) {
            boxInt(mv);
            return true;
        } else {
            return convertIntBaseTo(mv, toType);
        }
    }

    private static boolean convertByteTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Byte.class)) {
            boxByte(mv);
            return true;
        } else {
            return convertIntBaseTo(mv, toType);
        }
    }

    private static boolean convertShortTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Short.class)) {
            boxShort(mv);
            return true;
        } else {
            return convertIntBaseTo(mv, toType);
        }
    }

    private static boolean convertCharTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Character.class)) {
            boxChar(mv);
            return true;
        } else {
            return convertIntBaseTo(mv, toType);
        }
    }

    private static boolean convertIntBaseTo(MethodVisitor mv, Class<?> toType) {
        if (toType == Integer.class) {
            boxInt(mv);
        // Conversion from int to long
        } else if (toType == long.class) {
            mv.visitInsn(I2L);
        } else if (toType == Long.class) {
            mv.visitInsn(I2L);
            boxLong(mv);
        // Conversion from int to double
        } else if (toType == double.class) {
            mv.visitInsn(I2D);
        } else if (toType == Double.class) {
            mv.visitInsn(I2D);
            boxDouble(mv);
        // Conversion from int to float
        } else if (toType == float.class) {
            mv.visitInsn(I2F);
        } else if (toType == Float.class) {
            mv.visitInsn(I2F);
            boxFloat(mv);
        // Conversion from int to byte
        } else if (toType == byte.class) {
            mv.visitInsn(I2B);
        } else if (toType == Byte.class) {
            mv.visitInsn(I2B);
            boxByte(mv);
        // Conversion from int to short
        } else if (toType == short.class) {
            mv.visitInsn(I2S);
        } else if (toType == Short.class) {
            mv.visitInsn(I2S);
            boxShort(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertFloatTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Float.class)) {
            boxFloat(mv);
            // Conversion from float to long
        } else if (toType == long.class) {
            mv.visitInsn(F2L);
        } else if (toType == Long.class) {
            mv.visitInsn(F2L);
            boxLong(mv);
            // Conversion from float to double
        } else if (toType == double.class) {
            mv.visitInsn(F2D);
        } else if (toType == Double.class) {
            mv.visitInsn(F2D);
            boxDouble(mv);
            // Conversion from float to int
        } else if (toType == int.class) {
            mv.visitInsn(F2I);
        } else if (toType == Integer.class) {
            mv.visitInsn(F2I);
            boxInt(mv);
            // Conversion from float to byte
        } else if (toType == byte.class) {
            mv.visitInsn(F2I);
            mv.visitInsn(I2B);
        } else if (toType == Byte.class) {
            mv.visitInsn(F2I);
            mv.visitInsn(I2B);
            boxByte(mv);
            // Conversion from float to short
        } else if (toType == short.class) {
            mv.visitInsn(F2I);
            mv.visitInsn(I2S);
        } else if (toType == Short.class) {
            mv.visitInsn(F2I);
            mv.visitInsn(I2S);
            boxShort(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertDoubleTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Double.class)) {
            boxDouble(mv);
            // Conversion from double to long
        } else if (toType == long.class) {
            mv.visitInsn(D2L);
        } else if (toType == Long.class) {
            mv.visitInsn(D2L);
            boxLong(mv);
            // Conversion from double to float
        } else if (toType == float.class) {
            mv.visitInsn(D2F);
        } else if (toType == Float.class) {
            mv.visitInsn(D2F);
            boxFloat(mv);
            // Conversion from double to int
        } else if (toType == int.class) {
            mv.visitInsn(D2I);
        } else if (toType == Integer.class) {
            mv.visitInsn(D2I);
            boxInt(mv);
            // Conversion from double to byte
        } else if (toType == byte.class) {
            mv.visitInsn(D2I);
            mv.visitInsn(I2B);
        } else if (toType == Byte.class) {
            mv.visitInsn(D2I);
            mv.visitInsn(I2B);
            boxByte(mv);
            // Conversion from double to short
        } else if (toType == short.class) {
            mv.visitInsn(D2I);
            mv.visitInsn(I2S);
        } else if (toType == Short.class) {
            mv.visitInsn(D2I);
            mv.visitInsn(I2S);
            boxShort(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertLongTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Long.class)) {
            boxLong(mv);
            // Conversion from long to double
        } else if (toType == double.class) {
            mv.visitInsn(L2D);
        } else if (toType == Double.class) {
            mv.visitInsn(L2D);
            boxDouble(mv);
            // Conversion from long to float
        } else if (toType == float.class) {
            mv.visitInsn(L2F);
        } else if (toType == Float.class) {
            mv.visitInsn(L2F);
            boxFloat(mv);
            // Conversion from long to int
        } else if (toType == int.class) {
            mv.visitInsn(L2I);
        } else if (toType == Integer.class) {
            mv.visitInsn(L2I);
            boxInt(mv);
            // Conversion from long to byte
        } else if (toType == byte.class) {
            mv.visitInsn(L2I);
            mv.visitInsn(I2B);
        } else if (toType == Byte.class) {
            mv.visitInsn(L2I);
            mv.visitInsn(I2B);
            boxByte(mv);
            // Conversion from long to short
        } else if (toType == short.class) {
            mv.visitInsn(L2I);
            mv.visitInsn(I2S);
        } else if (toType == Short.class) {
            mv.visitInsn(L2I);
            mv.visitInsn(I2S);
            boxShort(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertBoxedNumberTo(MethodVisitor mv, Class<?> toType) {
        if (toType == long.class) {
            unboxNumberAsLong(mv);
        } else if (toType == Long.class) {
            unboxNumberAsLong(mv);
            boxLong(mv);
        } else if (toType == float.class) {
            unboxNumberAsFloat(mv);
        } else if (toType == Float.class) {
            unboxNumberAsFloat(mv);
            boxFloat(mv);
        } else if (toType == double.class) {
            unboxNumberAsDouble(mv);
        } else if (toType == Double.class) {
            unboxNumberAsDouble(mv);
            boxFloat(mv);
        } else if (toType == int.class) {
            unboxNumberAsInt(mv);
        } else if (toType == Integer.class) {
            unboxNumberAsInt(mv);
            boxInt(mv);
        } else if (toType == byte.class) {
            unboxNumberAsByte(mv);
        } else if (toType == Byte.class) {
            unboxNumberAsByte(mv);
            boxByte(mv);
        } else if (toType == short.class) {
            unboxNumberAsShort(mv);
        } else if (toType == Short.class) {
            unboxNumberAsShort(mv);
            boxShort(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertBooleanTo(MethodVisitor mv, Class<?> toType) {
        if (toType.isAssignableFrom(Boolean.class)) {
            boxBoolean(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertBoxedBooleanTo(MethodVisitor mv, Class<?> toType) {
        if (toType == boolean.class) {
            unboxBoolean(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertBoxedCharTo(MethodVisitor mv, Class<?> toType) {
        if (toType == char.class) {
            unboxBoolean(mv);
        } else {
            return false;
        }
        return true;
    }

    private static boolean convertObjectTo(MethodVisitor mv, Class<?> toType) {
        if (toType == int.class) {
            unboxNumberAsInt(mv);
        } else if (toType == byte.class) {
            unboxNumberAsByte(mv);
        } else if (toType == short.class) {
            unboxNumberAsShort(mv);
        } else if (toType == long.class) {
            unboxNumberAsLong(mv);
        } else if (toType == float.class) {
            unboxNumberAsFloat(mv);
        } else if (toType == double.class) {
            unboxNumberAsDouble(mv);
        } else if (toType == char.class) {
            unboxChar(mv);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(toType));
        }
        return true;
    }

    static void visitConversion(MethodVisitor mv, Class<?> fromType, Class<?> toType) {
        if (fromType == toType || toType.isAssignableFrom(fromType)) { // No need for conversion+
            return;
        }
        boolean success;
        if (fromType == int.class) {
            success = convertIntTo(mv, toType);
        } else if (fromType == byte.class) {
            success = convertByteTo(mv, toType);
        } else if (fromType == short.class) {
            success = convertShortTo(mv, toType);
        } else if (fromType == char.class) {
            success = convertCharTo(mv, toType);
        } else if (fromType == float.class) {
            success = convertFloatTo(mv, toType);
        } else if (fromType == double.class) {
            success = convertDoubleTo(mv, toType);
        } else if (fromType == long.class) {
            success = convertLongTo(mv, toType);
        } else if (fromType == boolean.class) {
            success = convertBooleanTo(mv, toType);
        } else if (fromType == Boolean.class) {
            success = convertBoxedBooleanTo(mv, toType);
        } else if (fromType == Character.class) {
            success = convertBoxedCharTo(mv, toType);
        } else if (Number.class.isAssignableFrom(toType)) {
            success = convertBoxedNumberTo(mv, toType);
        } else if (fromType.isPrimitive()) {
            throw new IllegalStateException("Missing conversion for primitive: " + fromType.getName());
        } else {
            success = convertObjectTo(mv, toType);
        }
        if (!success) {
            throw new IllegalStateException("Cannot convert " + fromType.getName() + " to " + toType.getName());
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
    static void visitLoad(MethodVisitor mv, Type type, int parameter) {
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
    static void visitReturn(MethodVisitor mv, Type type) {
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


    /**
     * Visits the {@link MethodVisitor} to push a
     * constant integer value to the stack.
     *
     * @param mv The method visitor
     * @param value The integer
     */
    static void visitPushInt(MethodVisitor mv, int value) {
        if (value == -1) {
            mv.visitInsn(ICONST_M1);
        } else if (value == 0) {
            mv.visitInsn(ICONST_0);
        } else if (value == 1) {
            mv.visitInsn(ICONST_1);
        } else if (value == 2) {
            mv.visitInsn(ICONST_2);
        } else if (value == 3) {
            mv.visitInsn(ICONST_3);
        } else if (value == 4) {
            mv.visitInsn(ICONST_4);
        } else if (value == 5) {
            mv.visitInsn(ICONST_5);
        } else if (value >= -128 && value <= 127) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value >= -32768 && value <= 32767) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    static void visitLoadType(MethodVisitor mv, Type type) {
        final int sort = type.getSort();
        if (sort == Type.INT) {
            visitLoadType(mv, "Integer");
        } else if (sort == Type.BYTE) {
            visitLoadType(mv, "Byte");
        } else if (sort == Type.BOOLEAN) {
            visitLoadType(mv, "Boolean");
        } else if (sort == Type.SHORT) {
            visitLoadType(mv, "Short");
        } else if (sort == Type.CHAR) {
            visitLoadType(mv, "Character");
        } else if (sort == Type.DOUBLE) {
            visitLoadType(mv, "Double");
        } else if (sort == Type.FLOAT) {
            visitLoadType(mv, "Float");
        } else if (sort == Type.LONG) {
            visitLoadType(mv, "Long");
        } else if (sort == Type.VOID) {
            visitLoadType(mv, "Void");
        } else {
            mv.visitLdcInsn(type);
        }
    }

    private static void visitLoadType(MethodVisitor mv, String name) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/" + name, "TYPE", "Ljava/lang/Class;");
    }
}
