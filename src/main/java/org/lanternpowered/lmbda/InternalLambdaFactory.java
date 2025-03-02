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
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

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
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Separated from {@link LambdaFactory} to keep it clean.
 */
final class InternalLambdaFactory {

  /**
   * The internal lookup that has access to this library package.
   */
  private static final MethodHandles.Lookup internalLookup = MethodHandles.lookup();

  /**
   * A counter to make sure that lambda names don't conflict.
   */
  private static final AtomicInteger lambdaCounter = new AtomicInteger();

  private static final AtomicInteger holderCounter = new AtomicInteger();
  private static final Map<Class<?>, Holder> holders = new WeakHashMap<>();
  private static final ReentrantLock holdersLock = new ReentrantLock();

  private static final class Holder {
    final Class<?> theClass;
    final String internalClassName;
    final ThreadLocal<MethodHandle> currentMethodHandle;

    Holder(Class<?> theClass, String internalClassName, ThreadLocal<MethodHandle> currentMethodHandle) {
      this.theClass = theClass;
      this.internalClassName = internalClassName;
      this.currentMethodHandle = currentMethodHandle;
    }
  }

  private static final @Nullable MethodHandle defineHiddenClass =
    InternalMethodHandles.findDefineHiddenClassMethodHandle();

  static <T> T create(
    LambdaType<T> lambdaType,
    MethodHandle methodHandle
  ) {
    requireNonNull(lambdaType, "lambdaType");
    requireNonNull(methodHandle, "methodHandle");

    MethodHandles.Lookup defineLookup = lambdaType.defineLookup;
    if (defineLookup == null) {
      defineLookup = internalLookup;
    }

    // Check that the lambda type can be defined using the lookup
    Class<?> functionClass = lambdaType.resolved.functionClass;

    // Check if the classes are in the same package, this is only a problem if the access isn't
    // public, or the constructor isn't public
    boolean samePackage = InternalUtilities.getPackageName(functionClass)
      .equals(InternalUtilities.getPackageName(defineLookup.lookupClass()));

    if (!Modifier.isPublic(functionClass.getModifiers()) && !samePackage) {
      throw throwUnchecked(new IllegalAccessException("The function class isn't public and no " +
        "applicable define lookup is provided. When the access isn't public, the defined class " +
        "must be in the same package, a lookup within the same package can be set using " +
        "LambdaType#defineClassesWith(...)"));
    } else if (!functionClass.isInterface()) {
      try {
        int modifiers = functionClass.getDeclaredConstructor().getModifiers();
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
      int modifiers = lambdaType.resolved.method.getModifiers();
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

  private static String toGenericDescriptor(
    Class<?> superClass,
    ParameterizedType genericType
  ) {
    Map<String, TypeVariable<?>> typeVariables = new HashMap<>();

    StringBuilder signatureBuilder = new StringBuilder();
    toGenericSignature(signatureBuilder, genericType, typeVariables);

    StringBuilder descriptorBuilder = new StringBuilder();

    if (!typeVariables.isEmpty()) {
      descriptorBuilder.append('<');
      for (TypeVariable<?> typeVariable : typeVariables.values()) {
        descriptorBuilder.append(typeVariable.getName());
        for (java.lang.reflect.Type bound : typeVariable.getBounds()) {
          if (isFinal(bound)) {
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

  private static boolean isFinal(java.lang.reflect.Type bound) {
    boolean isFinal;
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
    return isFinal;
  }

  private static void toGenericSignature(
    StringBuilder builder,
    java.lang.reflect.Type type,
    @Nullable Map<String, TypeVariable<?>> typeVariables
  ) {
    if (type instanceof Class) {
      builder.append(Type.getDescriptor((Class<?>) type));
    } else if (type instanceof GenericArrayType) {
      GenericArrayType arrayType = (GenericArrayType) type;
      builder.append('[');
      toGenericSignature(builder, arrayType.getGenericComponentType(), typeVariables);
    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      builder.append('L');
      builder.append(Type.getInternalName((Class<?>) parameterizedType.getRawType()));
      builder.append('<');
      for (java.lang.reflect.Type parameter : parameterizedType.getActualTypeArguments()) {
        toGenericSignature(builder, parameter, typeVariables);
      }
      builder.append('>').append(';');
    } else if (type instanceof TypeVariable) {
      TypeVariable<?> typeVariable = (TypeVariable<?>) type;
      builder.append('T').append(typeVariable.getName()).append(';');
      if (typeVariables != null) {
        typeVariables.put(typeVariable.getName(), typeVariable);
      }
    } else if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;

      java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();
      java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();

      boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
      boolean hasUpper = upperBounds != null && upperBounds.length > 0;

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
  private static <T> T createGeneratedFunction(
    ResolvedLambdaType<?> lambdaType,
    MethodHandle methodHandle,
    MethodHandles.Lookup defineLookup
  ) {
    // Convert the method handle types to match the functional method signature, this will make
    // sure that all the objects are converted accordingly, so we don't have to do it ourselves
    // with asm.
    // This will also throw an exception if the functional interface cannot be implemented by the
    // given method handle
    MethodType methodType = lambdaType.methodType;
    // drop parameters at the end if we have too many
    if (methodType.parameterCount() > methodHandle.type().parameterCount()) {
      methodType = methodType.dropParameterTypes(methodHandle.type().parameterCount(),
        methodType.parameterCount());
    }
    MethodHandle convertedMethodHandle = methodHandle.asType(methodType);

    Method method = lambdaType.method;
    ClassWriter cw = new ClassWriter(0);

    Class<?> functionClass = lambdaType.functionClass;
    Class<?> superclass = functionClass.isInterface() ? Object.class : functionClass;

    String genericDescriptor = null;
    if (lambdaType.genericFunctionType != null) {
      genericDescriptor = toGenericDescriptor(superclass, lambdaType.genericFunctionType);
    }

    String[] interfaces = !functionClass.isInterface() ? new String[0] :
      new String[] { Type.getInternalName(lambdaType.functionClass) };

    String internalClassName = generateInternalClassName(defineLookup, lambdaCounter, "Lmbda");

    Holder holder = createHolder(defineLookup);

    cw.visit(V1_8, ACC_SUPER, internalClassName, genericDescriptor,
      Type.getInternalName(superclass), interfaces);

    FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
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
    mv.visitFieldInsn(GETSTATIC, holder.internalClassName, "METHOD_HANDLE", "Ljava/lang/ThreadLocal;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false);
    mv.visitTypeInsn(CHECKCAST, "java/lang/invoke/MethodHandle");
    mv.visitFieldInsn(PUTSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME,
      "Ljava/lang/invoke/MethodHandle;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 0);
    mv.visitEnd();

    // Write the function method
    String descriptor = Type.getMethodDescriptor(method);
    mv = cw.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
    // Hide the lambda from the stack trace
    mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true).visitEnd();
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, internalClassName, METHOD_HANDLE_FIELD_NAME,
      "Ljava/lang/invoke/MethodHandle;");
    Class<?>[] parameters = method.getParameterTypes();
    int maxStack = 1;
    for (int i = 0; i < methodType.parameterCount(); i++) {
      Type type = Type.getType(parameters[i]);
      mv.visitVarInsn(type.getOpcode(ILOAD), maxStack);
      maxStack += type.getSize();
    }
    int maxLocals = maxStack;
    for (int i = methodType.parameterCount(); i < parameters.length; i++) {
      maxLocals += Type.getType(parameters[i]).getSize();
    }
    Type[] methodHandleParameterTypes =
      methodType.parameterList().stream().map(Type::getType).toArray(Type[]::new);
    String methodHandleDescriptor = Type.getMethodDescriptor(
      Type.getType(methodType.returnType()), methodHandleParameterTypes);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
      "invokeExact", methodHandleDescriptor, false);
    mv.visitInsn(Type.getType(method.getReturnType()).getOpcode(IRETURN));
    mv.visitMaxs(maxStack, maxLocals);
    mv.visitEnd();

    cw.visitEnd();

    ThreadLocal<MethodHandle> currentMethodHandle = holder.currentMethodHandle;
    try {
      // Store the current method handle, it will be required on initialization of the generated class
      currentMethodHandle.set(convertedMethodHandle);
      byte[] bytes = cw.toByteArray();
      MethodHandles.Lookup theClassLookup;
      Class<?> theClass;
      // Define the class within the provided lookup
      if (defineHiddenClass != null) {
        theClassLookup = doUnchecked(() -> (MethodHandles.Lookup) defineHiddenClass
          .invokeExact(defineLookup, bytes, true));
        theClass = theClassLookup.lookupClass();
      } else {
        theClass = doUnchecked(() ->
          MethodHandlesExtensions.defineClass(defineLookup, bytes));
        theClassLookup = defineLookup.in(theClass);
      }

      // Instantiate the function object
      return doUnchecked(() -> (T) theClassLookup
        .findConstructor(theClass, MethodType.methodType(void.class)).invoke());
    } finally {
      // Cleanup
      currentMethodHandle.remove();
    }
  }

  @SuppressWarnings("unchecked")
  private static Holder createHolder(MethodHandles.Lookup defineLookup) {
    Holder holder;
    holdersLock.lock();
    try {
      holder = holders.get(defineLookup.lookupClass());
    } finally {
      holdersLock.unlock();
    }
    if (holder != null) {
      return holder;
    }

    String internalClassName = generateInternalClassName(defineLookup, holderCounter, "Lmbda$MH");

    ClassWriter cw = new ClassWriter(0);
    cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
      internalClassName, null, "java/lang/Object", null);

    FieldVisitor fv = cw.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, "METHOD_HANDLE",
      "Ljava/lang/ThreadLocal;", "Ljava/lang/ThreadLocal<Ljava/lang/invoke/MethodHandle;>;", null);
    fv.visitEnd();

    MethodVisitor mv = cw.visitMethod(ACC_STATIC,
      "<clinit>", "()V", null, null);
    mv.visitCode();
    mv.visitTypeInsn(NEW, "java/lang/ThreadLocal");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false);
    mv.visitFieldInsn(PUTSTATIC, internalClassName,
      "METHOD_HANDLE", "Ljava/lang/ThreadLocal;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 0);
    mv.visitEnd();

    cw.visitEnd();

    byte[] bytes = cw.toByteArray();
    holder = doUnchecked(() -> {
      Class<?> holderClass = MethodHandlesExtensions.defineClass(defineLookup, bytes);
      ThreadLocal<MethodHandle> threadLocal;
      try {
        MethodHandle currentMethodHandleGetter = defineLookup
          .findStaticGetter(holderClass, "METHOD_HANDLE", ThreadLocal.class);
        threadLocal = (ThreadLocal<MethodHandle>) currentMethodHandleGetter.invokeExact();
      } catch (Throwable ex) {
        throw new IllegalStateException(ex);
      }
      return new Holder(holderClass, internalClassName, threadLocal);
    });

    holdersLock.lock();
    try {
      holders.put(defineLookup.lookupClass(), holder);
    } finally {
      holdersLock.unlock();
    }
    return holder;
  }

  private static String generateInternalClassName(MethodHandles.Lookup lookup, AtomicInteger counter, String type) {
    String packageName = InternalUtilities.getPackageName(lookup.lookupClass());
    String classPrefix = packageName.isEmpty() ? "" : packageName + ".";
    String className = classPrefix + type + '$' + counter.incrementAndGet();
    return className.replace('.', '/');
  }

  private InternalLambdaFactory() {
  }
}
