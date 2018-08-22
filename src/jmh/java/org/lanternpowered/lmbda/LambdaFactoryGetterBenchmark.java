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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

/**
 * Based on the original benchmark found on the stack overflow post:
 * <a href="https://stackoverflow.com/questions/22244402/how-can-i-improve-performance-of-field-set-perhap-using-methodhandles?noredirect=1&lq=1">
 *     How can I improve performance of Field.set (perhaps using MethodHandles)?</a>
 */
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class LambdaFactoryGetterBenchmark {

    private int value = 42;

    private static final Field static_reflective;
    private static final MethodHandle static_unreflect;
    private static final MethodHandle static_mh;

    private static Field reflective;
    private static MethodHandle unreflect;
    private static MethodHandle mh;

    private static ToIntFunction<LambdaFactoryGetterBenchmark> mh_function;
    private static ToIntFunction<LambdaFactoryGetterBenchmark> lmbda;

    private static ToIntFunction<LambdaFactoryGetterBenchmark> function_plain;

    // We would normally use @Setup, but we need to initialize "static final" fields here...
    static {
        try {
            reflective = LambdaFactoryGetterBenchmark.class.getDeclaredField("value");
            unreflect = MethodHandles.lookup().unreflectGetter(reflective);
            mh = MethodHandles.lookup().findGetter(LambdaFactoryGetterBenchmark.class, "value", int.class);
            static_reflective = reflective;
            static_unreflect = unreflect;
            static_mh = mh;
            function_plain = object -> object.value;
            // Create a manually implemented lambda to compare performance with generated ones.
            mh_function = object -> {
                try {
                    return (int) static_unreflect.invokeExact(object);
                } catch (Throwable t) {
                    throw MethodHandlesX.throwUnchecked(t);
                }
            };
            lmbda = LambdaFactory.create(FunctionalInterface.of(ToIntFunction.class), MethodHandles.lookup(), mh);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    @Benchmark
    public int plain() {
        return value;
    }

    @Benchmark
    public int dynamic_reflect() throws IllegalAccessException {
        return (int) reflective.get(this);
    }

    @Benchmark
    public int dynamic_unreflect_invoke() throws Throwable {
        return (int) unreflect.invoke(this);
    }

    @Benchmark
    public int dynamic_unreflect_invokeExact() throws Throwable {
        return (int) unreflect.invokeExact(this);
    }

    @Benchmark
    public int dynamic_mh_invoke() throws Throwable {
        return (int) mh.invoke(this);
    }

    @Benchmark
    public int dynamic_mh_invokeExact() throws Throwable {
        return (int) mh.invokeExact(this);
    }

    @Benchmark
    public int static_reflect() throws InvocationTargetException, IllegalAccessException {
        return (int) static_reflective.get(this);
    }

    @Benchmark
    public int static_unreflect_invoke() throws Throwable {
        return (int) static_unreflect.invoke(this);
    }

    @Benchmark
    public int static_unreflect_invokeExact() throws Throwable {
        return (int) static_unreflect.invokeExact(this);
    }

    @Benchmark
    public int static_mh_invoke() throws Throwable {
        return (int) static_mh.invoke(this);
    }

    @Benchmark
    public int static_mh_invokeExact() throws Throwable {
        return (int) static_mh.invokeExact(this);
    }

    @Benchmark
    public int static_mh_function() {
        return mh_function.applyAsInt(this);
    }

    @Benchmark
    public int function_plain_0() {
        return function_plain.applyAsInt(this);
    }

    @Benchmark
    public int lmbda_function() {
        return lmbda.applyAsInt(this);
    }
}
