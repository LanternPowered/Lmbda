/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
module org.lanternpowered.lmbda {
  exports org.lanternpowered.lmbda;
  exports org.lanternpowered.lmbda.mh;

  requires org.objectweb.asm;
  requires org.checkerframework.checker.qual;

  // Optional dependency for kotlin
  requires static kotlin.stdlib;
  requires static kotlin.reflect;
}
