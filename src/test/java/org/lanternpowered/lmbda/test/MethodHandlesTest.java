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
package org.lanternpowered.lmbda.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.MethodHandlesExtensions;
import org.lanternpowered.lmbda.test.other.Dummy;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;

final class MethodHandlesTest {

    @Test
    void testPrivateLookupInPrimitiveClass() {
        assertThrows(IllegalArgumentException.class, () -> MethodHandlesExtensions.privateLookupIn(byte.class, MethodHandles.lookup()));
    }

    @Test
    void testPrivateLookupInArrayClass() {
        assertThrows(IllegalArgumentException.class, () -> MethodHandlesExtensions.privateLookupIn(Dummy[].class, MethodHandles.lookup()));
    }

    @Test
    void testDefineNoPackageAccess() {
        final byte[] byteCode = generateSimpleByteCode("org.lanternpowered.lmbda.test.other.TestDefineNoPackageAccess");

        assertThrows(IllegalAccessException.class, () ->
                MethodHandlesExtensions.defineClass(MethodHandles.publicLookup().in(Dummy.class), byteCode));
        assertThrows(IllegalAccessException.class, () ->
                MethodHandlesExtensions.defineClass(MethodHandles.lookup().in(Dummy.class), byteCode));
    }

    @Test
    void testDefinePackageAccess() {
        final byte[] byteCode = generateSimpleByteCode("org.lanternpowered.lmbda.test.other.TestDefinePackageAccess");

        assertDoesNotThrow(() -> {
            final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(Dummy.class, MethodHandles.lookup());
            MethodHandlesExtensions.defineClass(lookup, byteCode);
        });
    }

    @Test
    void testDefineInWrongPackage() throws IllegalAccessException {
        final byte[] byteCode = generateSimpleByteCode("org.lanternpowered.lmbda.test.TestDefineWrongPackage");
        final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(Dummy.class, MethodHandles.lookup());

        assertThrows(IllegalArgumentException.class, () ->
                MethodHandlesExtensions.defineClass(lookup, byteCode));
    }

    /**
     * Generates bytecode for a empty class with the given name.
     *
     * @param className The class name
     * @return The generated byte code
     */
    private byte[] generateSimpleByteCode(String className) {
        final ClassWriter cw = new ClassWriter(0);

        cw.visit(V1_8, ACC_SUPER, className.replace('.', '/'),
                null, Type.getInternalName(Object.class), new String[0]);

        // Add a package private constructor
        final MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
