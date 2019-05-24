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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaType;

class AbstractLambdaFunctionTest {

    @Test
    void testFunction() throws Exception {
        final LambdaType<Function> testFunction = LambdaType.of(Function.class);

        assertEquals(testFunction.getFunctionClass(), Function.class);
        assertEquals(testFunction.getFunctionType(), Function.class);
        assertEquals(testFunction.getMethod(), Function.class.getDeclaredMethod("set", TestObject.class, Object.class));
    }

    @Test
    void testExtendedFunction() throws Exception {
        final LambdaType<ExtendedFunction> testFunction = LambdaType.of(ExtendedFunction.class);

        assertEquals(testFunction.getFunctionClass(), ExtendedFunction.class);
        assertEquals(testFunction.getFunctionType(), ExtendedFunction.class);
        assertEquals(testFunction.getMethod(), Function.class.getDeclaredMethod("set", TestObject.class, Object.class));
    }

    @Test
    void testOverrideMethodFunction() throws Exception {
        final LambdaType<OverrideMethodFunction> testFunction = LambdaType.of(OverrideMethodFunction.class);

        assertEquals(testFunction.getFunctionClass(), OverrideMethodFunction.class);
        assertEquals(testFunction.getFunctionType(), OverrideMethodFunction.class);
        assertEquals(testFunction.getMethod(), OverrideMethodFunction.class.getDeclaredMethod("set", TestObject.class, Object.class));
    }

    @Test
    void testNonAbstractMethodFunction() {
        assertThrows(IllegalStateException.class, () -> LambdaType.of(NonAbstractMethodFunction.class));
    }

    @Test
    void testOverrideNonAbstractMethodFunction() throws Exception {
        final LambdaType<OverrideNonAbstractMethodFunction> testFunction = LambdaType.of(OverrideNonAbstractMethodFunction.class);

        assertEquals(testFunction.getFunctionClass(), OverrideNonAbstractMethodFunction.class);
        assertEquals(testFunction.getFunctionType(), OverrideNonAbstractMethodFunction.class);
        assertEquals(testFunction.getMethod(), OverrideNonAbstractMethodFunction.class.getDeclaredMethod("set", TestObject.class, Object.class));
    }

    @Test
    void testMultipleAbstractMethodsFunction() {
        assertThrows(IllegalStateException.class, () -> LambdaType.of(MultipleAbstractMethodsFunction.class));
    }

    @Test
    void testNoMethodFunction() {
        assertThrows(IllegalStateException.class, () -> LambdaType.of(NoMethodFunction.class));
    }

    @Test
    void testGenericMethodFunction() throws Exception {
        final LambdaType<GenericFunction<Double>> testFunction = new LambdaType<GenericFunction<Double>>() {};

        assertEquals(testFunction.getFunctionClass(), GenericFunction.class);
        assertEquals(testFunction.getFunctionType(), new TypeToken<GenericFunction<Double>>() {}.getType());
        assertEquals(testFunction.getMethod(), GenericFunction.class.getDeclaredMethod("set", TestObject.class, Object.class));
    }

    public static class TestObject {
    }

    public static abstract class Function {

        public abstract void set(TestObject target, Object value);
    }

    public static abstract class ExtendedFunction extends Function {
    }

    public static abstract class OverrideMethodFunction extends ExtendedFunction {

        @Override
        public abstract void set(TestObject target, Object value);
    }

    public static abstract class NoMethodFunction {

    }

    public static abstract class NonAbstractMethodFunction {

        public void set(TestObject target, Object value) {
        }
    }

    public static abstract class OverrideNonAbstractMethodFunction extends NonAbstractMethodFunction {

        @Override
        public abstract void set(TestObject target, Object value);
    }

    public static abstract class MultipleAbstractMethodsFunction {

        public abstract void set(TestObject target, Object value);

        public abstract void otherSet(TestObject target, Object value);
    }

    public static abstract class GenericFunction<T> {

        public abstract void set(TestObject target, T value);
    }

    public static abstract class IntFunction extends GenericFunction<Integer> {
    }
}
