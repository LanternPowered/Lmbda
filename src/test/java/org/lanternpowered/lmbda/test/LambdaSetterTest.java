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
import java.lang.invoke.MethodType;
import java.util.function.BiFunction;
import java.util.function.ObjIntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaSetterTest {

  @Test
  void testField() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findSetter(TestObject.class, "data", int.class);

    final ObjIntConsumer<TestObject> setter = LambdaFactory.create(
      new LambdaType<ObjIntConsumer<TestObject>>() {}, methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, object.getData());
    setter.accept(object, 10000);
    assertEquals(10000, object.getData());
  }

  @Test
  void testMethod() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findVirtual(
      TestObject.class, "setData", MethodType.methodType(void.class, int.class));

    final BiFunction<Object, Object, Object> setter = LambdaFactory.createBiFunction(methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, object.getData());
    setter.apply(object, 10000);
    assertEquals(10000, object.getData());
  }

  public static class TestObject {

    private int data = 100;

    int getData() {
      return this.data;
    }

    void setData(int data) {
      this.data = data;
    }
  }
}
