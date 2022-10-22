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
import java.lang.reflect.Field;
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
public class IntGetterFieldBenchmark {

  @SuppressWarnings("FieldMayBeFinal")
  private int value = 32;


  private static final MethodHandle mhConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static MethodHandle mhDyn;

  private static final Field fieldConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static Field fieldDyn;

  private static final ToIntFunction<IntGetterFieldBenchmark> plainFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> fieldConstFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> fieldDynFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> mhConstFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> mhDynFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> mhProxyFunction;
  private static final ToIntFunction<IntGetterFieldBenchmark> lmbdaFunction;

  static {
    try {
      fieldConst = IntGetterFieldBenchmark.class.getDeclaredField("value");
      fieldDyn = fieldConst;
      mhConst = MethodHandles.lookup()
        .findGetter(IntGetterFieldBenchmark.class, "value", int.class);
      mhDyn = mhConst;

      plainFunction = object -> object.value;
      fieldConstFunction = object -> {
        try {
          return fieldConst.getInt(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      fieldDynFunction = object -> {
        try {
          return fieldDyn.getInt(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhConstFunction = object -> {
        try {
          return (int) mhConst.invokeExact(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhDynFunction = object -> {
        try {
          return (int) mhDyn.invokeExact(object);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhProxyFunction = MethodHandleProxies.asInterfaceInstance(ToIntFunction.class, mhDyn);
      lmbdaFunction = LambdaFactory.create(
        new LambdaType<ToIntFunction<IntGetterFieldBenchmark>>() {}, mhDyn);
    } catch (Throwable t) {
      throw throwUnchecked(t);
    }
  }

  @Benchmark
  public int direct() {
    return this.value;
  }

  @Benchmark
  public int plain() {
    return plainFunction.applyAsInt(this);
  }

  @Benchmark
  public int fieldConst() {
    return fieldConstFunction.applyAsInt(this);
  }

  @Benchmark
  public int fieldDyn() {
    return fieldDynFunction.applyAsInt(this);
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
  public int lmbda() {
    return lmbdaFunction.applyAsInt(this);
  }
}
