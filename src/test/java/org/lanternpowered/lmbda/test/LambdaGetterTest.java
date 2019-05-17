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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.ToIntFunction;

class LambdaGetterTest {

    @Test
    void test() throws Exception {
        final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
        final MethodHandle methodHandle = lookup.findGetter(TestObject.class, "data", int.class);

        final ToIntFunction<TestObject> getter = LambdaFactory.create(new LambdaType<ToIntFunction<TestObject>>() {}, methodHandle);

        final TestObject object = new TestObject();
        assertEquals(100, getter.applyAsInt(object));
        object.setData(10000);
        assertEquals(10000, getter.applyAsInt(object));
    }

    @Test
    void testGenericSignature() throws Exception {
        final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
        final MethodHandle methodHandle = lookup.findGetter(TestObject.class, "data", int.class);

        final ToIntFunction<TestObject> getter = LambdaFactory.create(new LambdaType<ToIntFunction<TestObject>>() {}, methodHandle);

        final TypeToken<?> paramType = TypeToken.of(getter.getClass()).resolveType(ToIntFunction.class.getTypeParameters()[0]);
        assertEquals(paramType.getRawType(), TestObject.class);
    }

    public static class TestObject {

        private int data = 100;

        void setData(int value) {
            this.data = value;
        }
    }
}
