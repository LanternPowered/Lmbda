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
import java.util.function.ObjLongConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaSetterTest {

  @Test
  void testFieldInt() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findSetter(TestObject.class, "dataInt", int.class);

    final ObjIntConsumer<TestObject> setter = LambdaFactory.create(
      new LambdaType<ObjIntConsumer<TestObject>>() {}, methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, object.getDataInt());
    setter.accept(object, 10000);
    assertEquals(10000, object.getDataInt());
  }

  @Test
  void testFieldLong() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findSetter(TestObject.class, "dataLong", long.class);

    final ObjLongConsumer<TestObject> setter = LambdaFactory.create(
      new LambdaType<ObjLongConsumer<TestObject>>() {}, methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, object.getDataLong());
    setter.accept(object, 10000);
    assertEquals(10000, object.getDataLong());
  }

  @Test
  void testMethod() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findVirtual(
      TestObject.class, "setDataInt", MethodType.methodType(void.class, int.class));

    final BiFunction<Object, Object, Object> setter = LambdaFactory.createBiFunction(methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, object.getDataInt());
    setter.apply(object, 10000);
    assertEquals(10000, object.getDataInt());
  }

  public static class TestObject {

    private int dataInt = 100;
    private long dataLong = 100;

    int getDataInt() {
      return this.dataInt;
    }

    void setDataInt(int dataInt) {
      this.dataInt = dataInt;
    }

    long getDataLong() {
      return this.dataLong;
    }

    void setDataLong(long dataLong) {
      this.dataLong = dataLong;
    }
  }
}
