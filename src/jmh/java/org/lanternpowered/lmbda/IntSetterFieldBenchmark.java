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
import java.util.function.ObjIntConsumer;

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
public class IntSetterFieldBenchmark {

  private int value = 32;

  private static final Field fieldConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static Field fieldDyn;

  private static final MethodHandle mhConst;
  @SuppressWarnings("FieldMayBeFinal")
  private static MethodHandle mhDyn;

  private static final ObjIntConsumer<IntSetterFieldBenchmark> plainFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> fieldConstFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> fieldDynFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> mhConstFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> mhDynFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> mhProxyFunction;
  private static final ObjIntConsumer<IntSetterFieldBenchmark> lmbdaFunction;

  static {
    try {
      fieldConst = IntSetterFieldBenchmark.class.getDeclaredField("value");
      fieldDyn = fieldConst;
      mhConst = MethodHandles.lookup()
        .findSetter(IntSetterFieldBenchmark.class, "value", int.class);
      mhDyn = mhConst;

      plainFunction = (object, value) -> object.value = value;
      fieldConstFunction = (object, value) -> {
        try {
          fieldConst.setInt(object, value);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      fieldDynFunction = (object, value) -> {
        try {
          fieldDyn.setInt(object, value);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhConstFunction = (object, value) -> {
        try {
          mhConst.invokeExact(object, value);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhDynFunction = (object, value) -> {
        try {
          mhDyn.invokeExact(object, value);
        } catch (Throwable t) {
          throw throwUnchecked(t);
        }
      };
      mhProxyFunction = MethodHandleProxies.asInterfaceInstance(ObjIntConsumer.class, mhDyn);
      lmbdaFunction = LambdaFactory.create(
        new LambdaType<ObjIntConsumer<IntSetterFieldBenchmark>>() {}, mhDyn);
    } catch (Throwable t) {
      throw throwUnchecked(t);
    }
  }

  @Benchmark
  public void direct(final Data data) {
    this.value = data.value++;
  }

  @Benchmark
  public void plain(final Data data) {
    plainFunction.accept(this, data.value++);
  }

  @Benchmark
  public void fieldConst(final Data data) {
    fieldConstFunction.accept(this, data.value++);
  }

  @Benchmark
  public void fieldDyn(final Data data) {
    fieldDynFunction.accept(this, data.value++);
  }

  @Benchmark
  public void methodHandleConst(final Data data) {
    mhConstFunction.accept(this, data.value++);
  }

  @Benchmark
  public void methodHandleDyn(final Data data) {
    mhDynFunction.accept(this, data.value++);
  }

  @Benchmark
  public void methodHandleProxy(final Data data) {
    mhProxyFunction.accept(this, data.value++);
  }

  @Benchmark
  public void lmbda(final Data data) {
    lmbdaFunction.accept(this, data.value++);
  }

  @State(Scope.Benchmark)
  public static class Data {

    public int value = 0;
  }
}
