/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
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
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

/**
 * Based on the original benchmark found on the stack overflow post:
 * <a href="https://stackoverflow.com/questions/22244402/how-can-i-improve-performance-of-field-set-perhap-using-methodhandles?noredirect=1&lq=1">
 * How can I improve performance of Field.set (perhaps using MethodHandles)?</a>
 */
@SuppressWarnings("unchecked")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class IntGetterMethodBenchmark {

  private int value = 42;

  private static final MethodHandle mhConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static MethodHandle mhDyn;

  private static final Method methodConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static Method methodDyn;

  private static final ToIntFunction<IntGetterMethodBenchmark> plainFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> methodConstFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> methodDynFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> mhConstFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> mhDynFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> mhProxyFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> lambdaMetafactoryFunction;
  private static final ToIntFunction<IntGetterMethodBenchmark> lmbdaFunction;

  static {
    try {
      methodDyn = IntGetterMethodBenchmark.class.getDeclaredMethod("getValue");
      mhDyn = MethodHandles.lookup().findVirtual(IntGetterMethodBenchmark.class,
        "getValue", MethodType.methodType(Integer.class));
      methodConst = methodDyn;
      mhConst = mhDyn;

      plainFunction = IntGetterMethodBenchmark::getValue;
      methodConstFunction = object -> {
        try {
          return (Integer) methodConst.invoke(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      methodDynFunction = object -> {
        try {
          return (Integer) methodDyn.invoke(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhConstFunction = object -> {
        try {
          return (Integer) mhConst.invokeExact(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhDynFunction = object -> {
        try {
          return (Integer) mhDyn.invokeExact(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhProxyFunction = MethodHandleProxies.asInterfaceInstance(ToIntFunction.class, mhDyn);
      lambdaMetafactoryFunction = JavaLambdaFactory.create(
        new LambdaType<ToIntFunction<IntGetterMethodBenchmark>>() {},
        MethodHandles.lookup(), mhDyn);
      lmbdaFunction = LambdaFactory.create(
        new LambdaType<ToIntFunction<IntGetterMethodBenchmark>>() {}, mhDyn);
    } catch (Throwable t) {
      throw throwUnchecked(t);
    }
  }

  private Integer getValue() {
    return this.value++;
  }

  @Benchmark
  public int direct() {
    return getValue();
  }

  @Benchmark
  public int plain() {
    return plainFunction.applyAsInt(this);
  }

  @Benchmark
  public int methodConst() {
    return methodConstFunction.applyAsInt(this);
  }

  @Benchmark
  public int methodDyn() {
    return methodDynFunction.applyAsInt(this);
  }

  @Benchmark
  public int methodHandleConst() {
    return mhConstFunction.applyAsInt(this);
  }

  @Benchmark
  public int methodHandleDyn() {
    return mhDynFunction.applyAsInt(this);
  }

  @Benchmark
  public int methodHandleProxy() {
    return mhProxyFunction.applyAsInt(this);
  }

  @Benchmark
  public int lambdaMetafactory() {
    return lambdaMetafactoryFunction.applyAsInt(this);
  }

  @Benchmark
  public int lmbda() {
    return lmbdaFunction.applyAsInt(this);
  }
}
