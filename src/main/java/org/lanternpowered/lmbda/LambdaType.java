/*
 * This file is part of Lmbda, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.lmbda;

import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Represents a function interface or abstract class
 * that can be be implemented by a generated function.
 */
public abstract class LambdaType<@NonNull T> {

    /**
     * Constructs a new {@link LambdaType} from the given function interface or
     * abstract class.
     *
     * <p>A functional abstract class is either a interface or a "abstract" class
     * that has a no-arg constructor. In both cases is only one abstract method
     * allowed.</p>
     *
     * @param functionType The function type
     * @param <T> The type of the function type
     * @return The lambda type
     * @throws IllegalArgumentException If no valid functional method could be found
     */
    public static <@NonNull T> @NonNull LambdaType<T> of(@NonNull Class<T> functionType) {
        requireNonNull(functionType, "functionType");
        return new Simple<>(functionType);
    }

    /**
     * Constructs a new {@link LambdaType} from the given function interface or
     * abstract class type.
     *
     * <p>A functional abstract class is either a interface or a "abstract" class
     * that has a no-arg constructor. In both cases is only one abstract method
     * allowed.</p>
     *
     * <p>The generic signature of the given {@link Type} will be retained by
     * a resulting function.</p>
     *
     * @param functionType The function type
     * @param <T> The type of the function
     * @return The lambda type
     * @throws IllegalArgumentException If no valid functional method could be found
     */
    public static <@NonNull T> @NonNull LambdaType<T> of(@NonNull Type functionType) {
        requireNonNull(functionType, "functionType");
        return new Simple<>(functionType);
    }

    /**
     * A simple implementation.
     */
    private static final class Simple<@NonNull T> extends LambdaType<T> {

        /**
         * Constructs a new {@link LambdaType}.
         *
         * @param functionType The function type
         */
        Simple(@NonNull Type functionType) {
            super(new ResolvedLambdaType<>(functionType), null);
        }

        /**
         * Constructs a new {@link LambdaType}.
         *
         * @param resolved The resolved lambda type
         * @param defineLookup The define lookup
         */
        Simple(@NonNull ResolvedLambdaType<T> resolved, MethodHandles.@Nullable Lookup defineLookup) {
            super(resolved, defineLookup);
        }
    }

    final @NonNull ResolvedLambdaType<T> resolved;

    /**
     * The lookup the generated lambda implementation will be
     * defined in. This is required when the target interface
     * doesn't provide public access that it can be implemented
     * outside of it's package.
     */
    final MethodHandles.@Nullable Lookup defineLookup;

    /**
     * Constructs a new {@link LambdaType}.
     *
     * <p>The generic signature from the extended class will be
     * used to determine the functional interface to implement.
     * If it's not resolved, a {@link IllegalStateException} can
     * be expected.</p>
     */
    public LambdaType() {
        final Class<?> theClass = getClass();
        final Class<?> superClass = theClass.getSuperclass();
        if (superClass != LambdaType.class) {
            throw new IllegalStateException("Only direct subclasses of LambdaType are allowed.");
        }
        final Type superType = theClass.getGenericSuperclass();
        if (!(superType instanceof ParameterizedType)) {
            throw new IllegalStateException("Direct subclasses of LambdaType must be a parameterized type.");
        }
        final ParameterizedType parameterizedType = (ParameterizedType) superType;
        this.resolved = new ResolvedLambdaType<>(parameterizedType.getActualTypeArguments()[0]);
        this.defineLookup = null;
    }

    /**
     * Constructs a new {@link LambdaType}.
     *
     * @param resolved The resolved lambda type
     * @param defineLookup The define lookup
     */
    private LambdaType(@NonNull ResolvedLambdaType<T> resolved, MethodHandles.@Nullable Lookup defineLookup) {
        this.defineLookup = defineLookup;
        this.resolved = resolved;
    }

    /**
     * Constructs a new {@link LambdaType} where that will define the
     * implementation class using the provided lookup.
     *
     * <p>When the functional interface/abstract class isn't public, or
     * the abstract method isn't, then should the lookup be defined explicitly.
     * Its implementation class needs the proper access in order to be implemented
     * successfully. This can be in the same package or a different one
     * depending on what is desired.</p>
     *
     * @param defineLookup The define lookup
     * @return The new lambda type
     */
    public final @NonNull LambdaType<T> defineClassesWith(MethodHandles.@NonNull Lookup defineLookup) {
        requireNonNull(defineLookup, "defineLookup");
        return new Simple<>(this.resolved, defineLookup);
    }

    /**
     * Gets the function class that will be implemented.
     *
     * @return The function class
     */
    public final @NonNull Class<T> getFunctionClass() {
        return this.resolved.functionClass;
    }

    /**
     * Gets the function type that will be implemented.
     *
     * @return The function type
     */
    public final @NonNull Type getFunctionType() {
        return this.resolved.getFunctionType();
    }

    /**
     * Gets the {@link Method} that will be implemented when
     * generating a function for this {@link LambdaType}.
     *
     * @return The method
     */
    public final @NonNull Method getMethod() {
        return this.resolved.getMethodCopy();
    }

    @Override
    public final @NonNull String toString() {
        return this.resolved.toString();
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (!(obj instanceof LambdaType)) {
            return false;
        }
        final LambdaType<?> that = (LambdaType<?>) obj;
        return that.resolved.equals(this.resolved) &&
                Objects.equals(that.defineLookup, this.defineLookup);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.resolved, this.defineLookup);
    }

    // Override these to force them as final

    @Override
    protected final @NonNull Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Deprecated
    @Override
    protected final void finalize() throws Throwable {
        super.finalize();
    }
}
