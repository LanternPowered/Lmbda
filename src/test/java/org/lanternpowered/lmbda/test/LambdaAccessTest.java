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

import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LambdaAccessTest {

  private MethodHandle getGetterMethodHandle() throws Exception {
    final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(
      TestObject.class, MethodHandles.lookup());
    return lookup.findGetter(TestObject.class, "data", int.class);
  }

  @Test
  void testPublicInterface() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // Private function classes aren't supported
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<IMyPublicFunction>() {}, methodHandle));
  }

  @Test
  void testPackagePrivateInterface() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    assertThrows(IllegalAccessException.class, () -> LambdaFactory.create(
      new LambdaType<IMyPackagePrivateFunction>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<IMyPackagePrivateFunction>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPrivateInterface() {
    // Private function interfaces aren't supported
    assertThrows(IllegalStateException.class, () -> new LambdaType<IMyPrivateFunction>() {});
  }

  @Test
  void testPrivate() {
    // Private function classes aren't supported
    assertThrows(IllegalStateException.class, () -> new LambdaType<MyPrivateFunction>() {});
  }

  @Test
  void testPublic() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // Every lookup should have access to public classes
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyPublicFunction>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyPublicFunction>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPackagePrivate() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function class
    assertThrows(IllegalAccessException.class, () -> LambdaFactory.create(
      new LambdaType<MyPackagePrivateFunction>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyPackagePrivateFunction>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtected() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the protected function class
    assertThrows(IllegalAccessException.class, () -> LambdaFactory.create(
      new LambdaType<MyProtectedFunction>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyProtectedFunction>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPrivateConstructor() {
    // Function classes with private constructors aren't supported
    assertThrows(IllegalStateException.class,
      () -> new LambdaType<MyFunctionWithPrivateConstructor>() {});
  }

  @Test
  void testPackagePrivateConstructor() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function constructor
    assertThrows(IllegalAccessException.class, () -> LambdaFactory.create(
      new LambdaType<MyFunctionWithPackagePrivateConstructor>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithPackagePrivateConstructor>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtectedConstructor() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithProtectedConstructor>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithProtectedConstructor>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPackagePrivateMethod() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function method
    assertThrows(IllegalAccessException.class, () -> LambdaFactory.create(
      new LambdaType<MyFunctionWithPackagePrivateMethod>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithPackagePrivateMethod>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtectedMethod() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithProtectedMethod>() {}, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<MyFunctionWithProtectedMethod>() {}
        .defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testInnerClass() throws Exception {
    final MethodHandle methodHandle = getGetterMethodHandle();

    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(
      new LambdaType<MyInnerFunction>() {}, methodHandle));
  }

  private static class TestObject {

    private int data = 100;
  }

  private interface IMyPrivateFunction {

    int getValue(TestObject testObject);
  }

  public interface IMyPublicFunction {

    int getValue(TestObject testObject);
  }

  interface IMyPackagePrivateFunction {

    int getValue(TestObject testObject);
  }

  @SuppressWarnings("InnerClassMayBeStatic")
  public abstract class MyInnerFunction {

    public abstract int getValue(TestObject testObject);
  }

  public abstract static class MyPublicFunction {

    public abstract int getValue(TestObject testObject);
  }

  private abstract static class MyPrivateFunction {

    public abstract int getValue(TestObject testObject);
  }

  abstract static class MyPackagePrivateFunction {

    public abstract int getValue(TestObject testObject);
  }

  abstract static class MyProtectedFunction {

    public abstract int getValue(TestObject testObject);
  }

  public abstract static class MyFunctionWithPrivateConstructor {

    private MyFunctionWithPrivateConstructor() {
    }

    public abstract int getValue(TestObject testObject);
  }

  public abstract static class MyFunctionWithPackagePrivateConstructor {

    MyFunctionWithPackagePrivateConstructor() {
    }

    public abstract int getValue(TestObject testObject);
  }

  public abstract static class MyFunctionWithProtectedConstructor {

    protected MyFunctionWithProtectedConstructor() {
    }

    public abstract int getValue(TestObject testObject);
  }

  public abstract static class MyFunctionWithPackagePrivateMethod {

    abstract int getValue(TestObject testObject);
  }

  public abstract static class MyFunctionWithProtectedMethod {

    protected abstract int getValue(TestObject testObject);
  }
}
