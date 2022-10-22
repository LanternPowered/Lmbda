/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.lmbda.test;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("UnstableApiUsage")
class AbstractLambdaFunctionTest {

  @Test
  void testImplementingFunction() throws Exception {
    final LambdaType<IFunctionImplementing> testFunction =
      LambdaType.of(IFunctionImplementing.class);

    assertEquals(testFunction.getFunctionClass(), IFunctionImplementing.class);
    assertEquals(testFunction.getFunctionType(), IFunctionImplementing.class);
    assertEquals(testFunction.getMethod(), IFunctionImplementing.class
      .getDeclaredMethod("getValueFrom", TestObject.class));
  }

  @Test
  void testFunction() throws Exception {
    final LambdaType<Function> testFunction = LambdaType.of(Function.class);

    assertEquals(testFunction.getFunctionClass(), Function.class);
    assertEquals(testFunction.getFunctionType(), Function.class);
    assertEquals(testFunction.getMethod(), Function.class
      .getDeclaredMethod("set", TestObject.class, Object.class));
  }

  @Test
  void testExtendedFunction() throws Exception {
    final LambdaType<ExtendedFunction> testFunction = LambdaType.of(ExtendedFunction.class);

    assertEquals(testFunction.getFunctionClass(), ExtendedFunction.class);
    assertEquals(testFunction.getFunctionType(), ExtendedFunction.class);
    assertEquals(testFunction.getMethod(), Function.class
      .getDeclaredMethod("set", TestObject.class, Object.class));
  }

  @Test
  void testOverrideMethodFunction() throws Exception {
    final LambdaType<OverrideMethodFunction> testFunction =
      LambdaType.of(OverrideMethodFunction.class);

    assertEquals(testFunction.getFunctionClass(), OverrideMethodFunction.class);
    assertEquals(testFunction.getFunctionType(), OverrideMethodFunction.class);
    assertEquals(testFunction.getMethod(), OverrideMethodFunction.class
      .getDeclaredMethod("set", TestObject.class, Object.class));
  }

  @Test
  void testNonAbstractMethodFunction() {
    assertThrows(IllegalStateException.class, () -> LambdaType.of(NonAbstractMethodFunction.class));
  }

  @Test
  void testOverrideNonAbstractMethodFunction() throws Exception {
    final LambdaType<OverrideNonAbstractMethodFunction> testFunction =
      LambdaType.of(OverrideNonAbstractMethodFunction.class);

    assertEquals(testFunction.getFunctionClass(), OverrideNonAbstractMethodFunction.class);
    assertEquals(testFunction.getFunctionType(), OverrideNonAbstractMethodFunction.class);
    assertEquals(testFunction.getMethod(), OverrideNonAbstractMethodFunction.class
      .getDeclaredMethod("set", TestObject.class, Object.class));
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
    final LambdaType<GenericFunction<Double>> testFunction =
      new LambdaType<GenericFunction<Double>>() {};

    assertEquals(testFunction.getFunctionClass(), GenericFunction.class);
    assertEquals(testFunction.getFunctionType(),
      new TypeToken<GenericFunction<Double>>() {}.getType());
    assertEquals(testFunction.getMethod(),
      GenericFunction.class.getDeclaredMethod("set", TestObject.class, Object.class));
  }

  public static class TestObject {
  }

  public interface IFunction {

    int getValue(TestObject testObject);
  }

  abstract static class IFunctionImplementing implements IFunction {

    @Override
    public int getValue(TestObject testObject) {
      return getValueFrom(testObject);
    }

    public abstract int getValueFrom(TestObject testObject);
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
