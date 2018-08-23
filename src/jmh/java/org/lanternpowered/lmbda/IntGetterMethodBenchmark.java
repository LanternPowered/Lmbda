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
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
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
public class IntGetterMethodBenchmark {

    private Integer value = 42;

    private static final MethodHandle staticMethodHandle;
    private static final Method staticReflective;

    private static MethodHandle methodHandle;
    private static Method reflective;

    private static ToIntFunction<IntGetterMethodBenchmark> plainFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> staticMethodHandleFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> staticReflectiveFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> methodHandleFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> reflectiveFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> proxyFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> lambdaFunction;
    private static ToIntFunction<IntGetterMethodBenchmark> lmbdaFunction;

    // We would normally use @Setup, but we need to initialize "static final" fields here...
    static {
        try {
            // Access method handles, etc.
            reflective = IntGetterMethodBenchmark.class.getDeclaredMethod("getValue");
            methodHandle = MethodHandles.lookup().findVirtual(
                            IntGetterMethodBenchmark.class, "getValue", MethodType.methodType(Integer.class));
            staticReflective = reflective;
            staticMethodHandle = methodHandle;

            // Generate functions
            plainFunction = object -> object.value;
            staticMethodHandleFunction = object -> {
                try {
                    return (Integer) staticMethodHandle.invokeExact(object);
                } catch (Throwable t) {
                    throw MethodHandlesX.throwUnchecked(t);
                }
            };
            staticReflectiveFunction = object -> {
                try {
                    return (int) staticReflective.invoke(object);
                } catch (Throwable t) {
                    throw MethodHandlesX.throwUnchecked(t);
                }
            };
            methodHandleFunction = object -> {
                try {
                    return (Integer) methodHandle.invokeExact(object);
                } catch (Throwable t) {
                    throw MethodHandlesX.throwUnchecked(t);
                }
            };
            reflectiveFunction = object -> {
                try {
                    return (int) reflective.invoke(object);
                } catch (Throwable t) {
                    throw MethodHandlesX.throwUnchecked(t);
                }
            };
            //noinspection unchecked
            proxyFunction = MethodHandleProxies.asInterfaceInstance(ToIntFunction.class, methodHandle);
            lambdaFunction = InternalLambdaFactory.createLambda(
                    LmbdaType.of(ToIntFunction.class), MethodHandles.lookup(), methodHandle);
            lmbdaFunction = LmbdaFactory.create(LmbdaType.of(ToIntFunction.class), methodHandle);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private Integer getValue() {
        return this.value;
    }

    @Benchmark
    public int plain() {
        return plainFunction.applyAsInt(this);
    }

    @Benchmark
    public int static_mh() {
        return staticMethodHandleFunction.applyAsInt(this);
    }

    @Benchmark
    public int static_reflect() {
        return staticReflectiveFunction.applyAsInt(this);
    }

    @Benchmark
    public int dynamic_mh() {
        return methodHandleFunction.applyAsInt(this);
    }

    @Benchmark
    public int dynamic_reflect() {
        return reflectiveFunction.applyAsInt(this);
    }

    @Benchmark
    public int proxy() {
        return proxyFunction.applyAsInt(this);
    }

    @Benchmark
    public int lambda() {
        return lambdaFunction.applyAsInt(this);
    }

    @Benchmark
    public int lmbda() {
        return lmbdaFunction.applyAsInt(this);
    }
}
