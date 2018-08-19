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

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class LambdaFactoryTest {

    @Test
    public void testIntConsumer1() throws NoSuchMethodException {
        final Method method = Methods.class.getMethod("consumeInt", int.class);
        final IntConsumer intConsumer = LambdaFactory.create(FunctionalInterface.of(IntConsumer.class), method);
        intConsumer.accept(100);
    }

    @Test
    public void testIntConsumer2() throws NoSuchMethodException {
        final Method method = Methods.class.getMethod("consumeByte", Byte.class);
        final IntConsumer intConsumer = LambdaFactory.create(FunctionalInterface.of(IntConsumer.class), method);
        intConsumer.accept(1000);
    }

    @Test
    public void testIntConsumer3() throws NoSuchMethodException {
        final Method method = Methods.class.getMethod("consumeDouble", double.class);
        final IntConsumer intConsumer = LambdaFactory.create(FunctionalInterface.of(IntConsumer.class), method);
        intConsumer.accept(1000);
    }

    @Test
    public void testLongConsumer1() throws NoSuchMethodException {
        final Method method = Methods.class.getMethod("consumeInt", int.class);
        final LongConsumer longConsumer = LambdaFactory.create(FunctionalInterface.of(LongConsumer.class), method);
        longConsumer.accept(1000);
    }

    public static class Methods {

        static int value;

        public static void consumeInt(int i) {
            System.out.println("consumeInt: " + i);
        }

        public static void consumeByte(Byte i) {
            System.out.println("consumeByte: " + i);
        }

        public static void consumeDouble(double i) {
            System.out.println("consumeDouble: " + i);
        }

        public static void consumeLong(long i) {
            System.out.println("consumeLong: " + i);
        }
    }
}
