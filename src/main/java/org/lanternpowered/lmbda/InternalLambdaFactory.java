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

import static java.util.Objects.requireNonNull;
import static org.lanternpowered.lmbda.InternalUtilities.doUnchecked;
import static org.lanternpowered.lmbda.InternalUtilities.throwUnchecked;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Separated from {@link LambdaFactory} to keep it clean.
 */
public final class InternalLambdaFactory {

  /**
   * Requests a {@link MethodHandle} for the construction of a lambda implementation.
   *
   * <p>Calling this method outside of construction phase will result in an
   * {@link IllegalStateException}.</p>
   *
   * @return The method handle
   * @deprecated Should not be used directly, internal use only
   */
  @Deprecated
  public static @NonNull MethodHandle requestMethodHandle() {
    final MethodHandle methodHandle = currentMethodHandle.get();
    if (methodHandle != null) {
      return methodHandle;
    }
    throw new IllegalStateException("Illegal method handle request.");
  }

  /**
   * The internal lookup that has access to this library package.
   */
  private static final MethodHandles.@NonNull Lookup internalLookup =
    AccessController.doPrivileged((PrivilegedAction<MethodHandles.Lookup>) MethodHandles::lookup);

  /**
   * A thread local which temporarily holds the {@link MethodHandle}
   * that will be injected into the generated class.
   */
  private static final @NonNull ThreadLocal<MethodHandle> currentMethodHandle = new ThreadLocal<>();

  /**
   * A counter to make sure that lambda names don't conflict.
   */
  private static final @NonNull AtomicInteger lambdaCounter = new AtomicInteger();

  static <@NonNull T> T create(
    final @NonNull LambdaType<T> lambdaType,
    final @NonNull MethodHandle methodHandle
  ) {
    requireNonNull(lambdaType, "lambdaType");
    requireNonNull(methodHandle, "methodHandle");

    MethodHandles.Lookup defineLookup = lambdaType.defineLookup;
    if (defineLookup == null) {
      defineLookup = internalLookup;
    }

    // Check that the lambda type can be defined using the lookup
    final Class<?> functionClass = lambdaType.resolved.functionClass;

    // Check if the classes are in the same package, this is only a problem if the access isn't
    // public, or the constructor isn't public
    final boolean samePackage = InternalUtilities.getPackageName(functionClass)
      .equals(InternalUtilities.getPackageName(defineLookup.lookupClass()));

    if (!Modifier.isPublic(functionClass.getModifiers()) && !samePackage) {
      throw throwUnchecked(new IllegalAccessException("The function class isn't public and no " +
        "applicable define lookup is provided. When the access isn't public, the defined class " +
        "must be in the same package, a lookup within the same package can be set using " +
        "LambdaType#defineClassesWith(...)"));
    } else if (!functionClass.isInterface()) {
      try {
        final int modifiers = functionClass.getDeclaredConstructor().getModifiers();
        if (!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) && !samePackage) {
          throw throwUnchecked(new IllegalAccessException("The function class constructor isn't " +
            "public and no applicable define lookup is  provided. When the access isn't public, " +
            "the defined class must be in the same package, a lookup within the same package can " +
            "be set using LambdaType#defineClassesWith(...)"));
        }
      } catch (NoSuchMethodException e) {
        // Should never happen, is already checked for at the construction of lambda type
        throw throwUnchecked(e);
      }
      final int modifiers = lambdaType.resolved.method.getModifiers();
      if (!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) && !samePackage) {
        throw throwUnchecked(new IllegalAccessException("The function class method isn't public " +
          "or protected and no applicable define lookup is provided. When the access isn't " +
          "public, the defined class must be in the same package, a lookup within the same " +
          "package can be set using LambdaType#defineClassesWith(...)"));
      }
    }

    try {
      return createGeneratedFunction(lambdaType.resolved, methodHandle, defineLookup);
    } catch (Throwable e) {
      throw new IllegalStateException("Couldn't create lambda for: \"" + methodHandle + "\". "
        + "Failed to implement: " + lambdaType, e);
    }
  }

  private static @NonNull String toGenericDescriptor(
    final @NonNull Class<?> superClass,
    final @NonNull ParameterizedType genericType
  ) {
    final Map<String, TypeVariable<?>> typeVariables = new HashMap<>();

    final StringBuilder signatureBuilder = new StringBuilder();
    toGenericSignature(signatureBuilder, genericType, typeVariables);

    final StringBuilder descriptorBuilder = new StringBuilder();

    if (!typeVariables.isEmpty()) {
      descriptorBuilder.append('<');
      for (final TypeVariable<?> typeVariable : typeVariables.values()) {
        descriptorBuilder.append(typeVariable.getName());
        for (final java.lang.reflect.Type bound : typeVariable.getBounds()) {
          final boolean isFinal;
          if (bound instanceof Class) {
            isFinal = Modifier.isFinal(((Class<?>) bound).getModifiers());
          } else if (bound instanceof GenericArrayType) {
            throw new IllegalStateException(); // Should never happen
          } else if (bound instanceof ParameterizedType) {
            isFinal = Modifier.isFinal(
              ((Class<?>) ((ParameterizedType) bound).getRawType()).getModifiers());
          } else {
            isFinal = false;
          }
          if (isFinal) {
            descriptorBuilder.append(':');
          }
          descriptorBuilder.append(':');
          toGenericSignature(descriptorBuilder, bound, null);
        }
      }
      descriptorBuilder.append('>');
    }

    descriptorBuilder.append(Type.getDescriptor(superClass));
    descriptorBuilder.append(signatureBuilder);

    return descriptorBuilder.toString();
  }

  private static void toGenericSignature(
    final @NonNull StringBuilder builder,
    final java.lang.reflect.@NonNull Type type,
    final @Nullable Map<String, TypeVariable<?>> typeVariables
  ) {
    if (type instanceof Class) {
      builder.append(Type.getDescriptor((Class<?>) type));
    } else if (type instanceof GenericArrayType) {
      final GenericArrayType arrayType = (GenericArrayType) type;
      builder.append('[');
      toGenericSignature(builder, arrayType.getGenericComponentType(), typeVariables);
    } else if (type instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType) type;
      builder.append('L');
      builder.append(Type.getInternalName((Class<?>) parameterizedType.getRawType()));
      builder.append('<');
      for (java.lang.reflect.Type parameter : parameterizedType.getActualTypeArguments()) {
        toGenericSignature(builder, parameter, typeVariables);
      }
      builder.append('>').append(';');
    } else if (type instanceof TypeVariable) {
      final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
      builder.append('T').append(typeVariable.getName()).append(';');
      if (typeVariables != null) {
        typeVariables.put(typeVariable.getName(), typeVariable);
      }
    } else if (type instanceof WildcardType) {
      final WildcardType wildcardType = (WildcardType) type;

      final java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();
      final java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();

      final boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
      final boolean hasUpper = upperBounds != null && upperBounds.length > 0;

      if (hasUpper && hasLower &&
        Object.class.equals(lowerBounds[0]) &&
        Object.class.equals(upperBounds[0])
      ) {
        builder.append('*');
      } else if (hasLower) {
        builder.append('-');
        for (final java.lang.reflect.Type lower : lowerBounds) {
          toGenericSignature(builder, lower, typeVariables);
        }
      } else if (hasUpper) {
        if (upperBounds.length == 1 && Object.class.equals(upperBounds[0])) {
          builder.append('*');
        } else {
          builder.append('+');
          for (final java.lang.reflect.Type upper : upperBounds) {
            toGenericSignature(builder, upper, typeVariables);
          }
        }
      } else {
        builder.append('*');
      }
    }
  }

  private static final String METHOD_HANDLE_FIELD_NAME = "methodHandle";

  @SuppressWarnings("unchecked")
  private static <@NonNull T> T createGeneratedFunction(
    final @NonNull ResolvedLambdaType<?> lambdaType,
    final @NonNull MethodHandle methodHandle,
    final MethodHandles.@NonNull Lookup defineLookup
  ) {
    // Convert the method handle types to match the functional method signature, this will make
    // sure that all the objects are converted accordingly, so we don't have to do it ourselves
    // with asm.
    // This will also throw an exception if the functional interface cannot be implemented by the
    // given method handle
    final MethodHandle convertedMethodHandle = methodHandle.asType(lambdaType.methodType);

    final Method method = lambdaType.method;
    final ClassWriter cw = new ClassWriter(0);

    final String packageName = InternalUtilities.getPackageName(defineLookup.lookupClass());

    final String classPrefix = packageName.isEmpty() ? "" : packageName + ".";
    final String className = classPrefix + "Lmbda$" + lambdaCounter.incrementAndGet();
    final String internalClassName = className.replace('.', '/');

    final Class<?> functionClass = lambdaType.functionClass;
    final Class<?> superclass = functionClass.isInterface() ? Object.class : functionClass;

    String genericDescriptor = null;
    if (lambdaType.genericFunctionType != null) {
      genericDescriptor = toGenericDescriptor(superclass, lambdaType.genericFunctionType);
    }

    final String[] interfaces = !functionClass.isInterface() ? new String[0] :
      new String[] { Type.getInternalName(lambdaType.functionClass) };

    cw.visit(V1_8, ACC_SUPER, internalClassName, genericDescriptor,
      Type.getInternalName(superclass), interfaces);

    final FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
      METHOD_HANDLE_FIELD_NAME, "Ljava/lang/invoke/MethodHandle;", null, null);
    fv.visitEnd();

    // Add a package private constructor
    MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superclass), "<init>", "()V", false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    // Add the method handle field
    mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
    mv.visitCode();
    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(InternalLambdaFactory.class),
      "requestMethodHandle", "()Ljava/lang/invoke/MethodHandle;", false);
    mv.visitFieldInsn(PUTSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME,
      "Ljava/lang/invoke/MethodHandle;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 0);
    mv.visitEnd();

    // Write the function method
    final String descriptor = Type.getMethodDescriptor(method);
    mv = cw.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
    // Hide the lambda from the stack trace
    mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true).visitEnd();
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME,
      "Ljava/lang/invoke/MethodHandle;");
    final Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      mv.visitVarInsn(Type.getType(parameters[i]).getOpcode(ILOAD), 1 + i);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
      "invokeExact", descriptor, false);
    mv.visitInsn(Type.getType(method.getReturnType()).getOpcode(IRETURN));
    final int maxs = parameters.length + 1;
    mv.visitMaxs(maxs, maxs);
    mv.visitEnd();

    cw.visitEnd();

    try {
      // Store the current method handle, it will be required on initialization of the generated
      // class
      currentMethodHandle.set(convertedMethodHandle);

      // Define the class within the provided lookup
      final Class<?> theClass = AccessController.doPrivileged(
        (PrivilegedAction<Class<?>>) () -> doUnchecked(() ->
          MethodHandlesExtensions.defineClass(defineLookup, cw.toByteArray())
        ));

      // Instantiate the function object
      return doUnchecked(() -> (T) defineLookup.in(theClass)
        .findConstructor(theClass, MethodType.methodType(void.class)).invoke());
    } finally {
      // Cleanup
      currentMethodHandle.remove();
    }
  }

  private InternalLambdaFactory() {
  }
}
