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

  requires org.objectweb.asm; // Depends on the asm library

  requires static kotlin.stdlib; // Optional dependency for kotlin
  requires static com.google.common; // Optional dependency for guava
}
