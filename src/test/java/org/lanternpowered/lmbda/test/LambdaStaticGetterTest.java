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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.IntSupplier;

class LambdaStaticGetterTest {

  @Test
  void test() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle =
      lookup.findStaticGetter(TestObject.class, "data", int.class);

    final IntSupplier getter = LambdaFactory.create(LambdaType.of(IntSupplier.class), methodHandle);

    assertEquals(100, getter.getAsInt());
    TestObject.setData(10000);
    assertEquals(10000, getter.getAsInt());
  }

  public static class TestObject {

    private static int data = 100;

    static void setData(int value) {
      data = value;
    }
  }
}
