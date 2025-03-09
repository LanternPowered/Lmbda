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
    MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(
      TestObject.class, MethodHandles.lookup());
    return lookup.findGetter(TestObject.class, "data", int.class);
  }

  @Test
  void testPublicInterface() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    assertDoesNotThrow(() -> LambdaFactory.create(
      new LambdaType<IMyPublicFunction>() {}, methodHandle));
  }

  @Test
  void testPackagePrivateInterface() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    assertDoesNotThrow(() -> new LambdaType<IMyPackagePrivateFunction>() {});
    LambdaType<IMyPackagePrivateFunction> lambdaType = new LambdaType<IMyPackagePrivateFunction>() {};
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPrivateInterface() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the private function class
    LambdaType<IMyPrivateFunction> lambdaType = new LambdaType<IMyPrivateFunction>() {};
    assertDoesNotThrow(() -> new LambdaType<IMyPrivateFunction>() {});
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPrivate() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // Private abstract class have by default private constructors
    assertThrows(IllegalStateException.class, () -> new LambdaType<MyPrivateFunction>() {});
    // The default lookup doesn't have access to the private function class
    LambdaType<MyPrivateFunctionWithPackagePrivateConstructor> lambdaType =
      new LambdaType<MyPrivateFunctionWithPackagePrivateConstructor>() {};
    assertDoesNotThrow(() -> new LambdaType<MyPrivateFunctionWithPackagePrivateConstructor>() {});
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPublic() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // Every lookup should have access to public classes
    LambdaType<MyPublicFunction> lambdaType = new LambdaType<MyPublicFunction>() {};
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPackagePrivate() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function class
    LambdaType<MyPackagePrivateFunction> lambdaType = new LambdaType<MyPackagePrivateFunction>() {};
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtected() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the protected function class
    LambdaType<MyProtectedFunction> lambdaType = new LambdaType<MyProtectedFunction>() {};
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPrivateConstructor() {
    // Function classes with private constructors aren't supported
    assertThrows(IllegalStateException.class, () -> new LambdaType<MyFunctionWithPrivateConstructor>() {});
  }

  @Test
  void testPackagePrivateConstructor() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function constructor
    LambdaType<MyFunctionWithPackagePrivateConstructor> lambdaType =
      new LambdaType<MyFunctionWithPackagePrivateConstructor>() {};
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtectedConstructor() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    LambdaType<MyFunctionWithProtectedConstructor> lambdaType = new LambdaType<MyFunctionWithProtectedConstructor>() {};
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testPackagePrivateMethod() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    // The default lookup doesn't have access to the package private function method
    LambdaType<MyFunctionWithPackagePrivateMethod> lambdaType = new LambdaType<MyFunctionWithPackagePrivateMethod>() {};
    assertThrows(IllegalStateException.class, () -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testProtectedMethod() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

    LambdaType<MyFunctionWithProtectedMethod> lambdaType = new LambdaType<MyFunctionWithProtectedMethod>() {};
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType, methodHandle));
    assertDoesNotThrow(() -> LambdaFactory.create(lambdaType.defineClassesWith(MethodHandles.lookup()), methodHandle));
  }

  @Test
  void testInnerClass() throws Exception {
    MethodHandle methodHandle = getGetterMethodHandle();

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

  private abstract static class MyPrivateFunctionWithPackagePrivateConstructor {

    MyPrivateFunctionWithPackagePrivateConstructor() {
    }

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
