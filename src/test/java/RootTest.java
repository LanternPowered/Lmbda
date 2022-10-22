/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;
import org.lanternpowered.lmbda.test.MethodHandlesTest;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RootTest {

  @Test
  void testMethodHandlesDefineInRootPackage() throws IllegalAccessException {
    final byte[] byteCode = MethodHandlesTest.generateSimpleByteCode("RootAccessClass");
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(RootDummy.class, MethodHandles.lookup());

    assertDoesNotThrow(() -> MethodHandlesExtensions.defineClass(lookup, byteCode));
  }

  @Test
  void testRootLambdaImplementation() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(RootTestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle =
      lookup.findGetter(RootTestObject.class, "data", int.class);

    final ToIntFunction<RootTestObject> getter = LambdaFactory.create(
      new LambdaType<ToIntFunction<RootTestObject>>() {}, methodHandle);

    assertEquals(100, getter.applyAsInt(new RootTestObject()));
  }

  @Test
  void testRootLambdaImplementationDefineInRoot() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(RootTestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle =
      lookup.findGetter(RootTestObject.class, "data", int.class);

    final ToIntFunction<RootTestObject> getter = LambdaFactory.create(
      new LambdaType<ToIntFunction<RootTestObject>>() {}
        .defineClassesWith(lookup), methodHandle);

    assertEquals(100, getter.applyAsInt(new RootTestObject()));
  }
}
