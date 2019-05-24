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

import static org.lanternpowered.lmbda.InternalUtilities.throwUnchecked;

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
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Based on the original benchmark found on the stack overflow post:
 * <a href="https://stackoverflow.com/questions/22244402/how-can-i-improve-performance-of-field-set-perhap-using-methodhandles?noredirect=1&lq=1">
 *     How can I improve performance of Field.set (perhaps using MethodHandles)?</a>
 */
@SuppressWarnings("unchecked")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class IntSetterFieldBenchmark {

    private int value = 32;

    private static final Field staticReflective;
    private static final MethodHandle staticMethodHandle;

    private static Field reflective;
    private static MethodHandle methodHandle;

    private static IntSetFunction<IntSetterFieldBenchmark> plainFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> staticMethodHandleFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> staticReflectiveFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> methodHandleFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> reflectiveFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> proxyFunction;
    private static IntSetFunction<IntSetterFieldBenchmark> lmbdaFunction;

    public interface IntSetFunction<T> {

        void apply(T target, int value);
    }

    // We would normally use @Setup, but we need to initialize "static final" fields here...
    static {
        try {
            reflective = IntSetterFieldBenchmark.class.getDeclaredField("value");
            methodHandle = MethodHandles.lookup().findSetter(IntSetterFieldBenchmark.class, "value", int.class);
            staticReflective = reflective;
            staticMethodHandle = methodHandle;
            // Create a manually implemented lambda to compare performance with generated ones.
            plainFunction = (object, value) -> object.value = value;
            methodHandleFunction = (object, value) -> {
                try {
                    methodHandle.invokeExact(object, value);
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            };
            staticMethodHandleFunction = (object, value) -> {
                try {
                    staticMethodHandle.invokeExact(object, value);
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            };
            staticReflectiveFunction = (object, value) -> {
                try {
                    staticReflective.setInt(object, value);
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            };
            methodHandleFunction = (object, value) -> {
                try {
                    methodHandle.invokeExact(object, value);
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            };
            reflectiveFunction = (object, value) -> {
                try {
                    reflective.setInt(object, value);
                } catch (Throwable t) {
                    throw throwUnchecked(t);
                }
            };
            proxyFunction = MethodHandleProxies.asInterfaceInstance(IntSetFunction.class, methodHandle);
            lmbdaFunction = LambdaFactory.create(new LambdaType<IntSetFunction<IntSetterFieldBenchmark>>() {}, methodHandle);
        } catch (Throwable t) {
            throw throwUnchecked(t);
        }
    }

    @Benchmark
    public void direct(Data data) {
        this.value = data.value++;
    }

    @Benchmark
    public void plain(Data data) {
        plainFunction.apply(this, data.value++);
    }

    @Benchmark
    public void dynamicReflect(Data data) {
        reflectiveFunction.apply(this, data.value++);
    }

    @Benchmark
    public void dynamicMethodHandle(Data data) {
        methodHandleFunction.apply(this, data.value++);
    }

    @Benchmark
    public void staticReflect(Data data) {
        staticReflectiveFunction.apply(this, data.value++);
    }

    @Benchmark
    public void staticMethodHandle(Data data) {
        staticMethodHandleFunction.apply(this, data.value++);
    }

    @Benchmark
    public void proxy(Data data) {
        proxyFunction.apply(this, data.value++);
    }

    @Benchmark
    public void lmbda(Data data) {
        lmbdaFunction.apply(this, data.value++);
    }

    @State(Scope.Benchmark)
    public static class Data {

        public int value = 0;
    }
}
