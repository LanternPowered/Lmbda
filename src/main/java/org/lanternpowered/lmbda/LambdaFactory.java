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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A factory to create lambda functions from a given {@link MethodHandle}.
 */
@SuppressWarnings("unchecked")
public final class LambdaFactory {

    // Supplier

    private static final LambdaType<Supplier> supplierInterface = LambdaType.of(Supplier.class);
    private static final LambdaType<IntSupplier> intSupplierInterface = LambdaType.of(IntSupplier.class);

    // Functions

    private static final LambdaType<Function> functionInterface = LambdaType.of(Function.class);
    private static final LambdaType<BiFunction> biFunctionInterface = LambdaType.of(BiFunction.class);

    // Consumers

    private static final LambdaType<Consumer> consumerInterface = LambdaType.of(Consumer.class);
    private static final LambdaType<BiConsumer> biConsumerInterface = LambdaType.of(BiConsumer.class);

    // Predicates

    private static final LambdaType<Predicate> predicateInterface = LambdaType.of(Predicate.class);
    private static final LambdaType<BiPredicate> biPredicateInterface = LambdaType.of(BiPredicate.class);

    /**
     * Attempts to create a {@link Predicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the predicate
     * @param <U> The second input type of the predicate
     * @return The created bi predicate
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U> @NonNull BiPredicate<T, U> createBiPredicate(@NonNull MethodHandle methodHandle) {
        return create(biPredicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Predicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the predicate
     * @return The created predicate
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull Predicate<T> createPredicate(@NonNull MethodHandle methodHandle) {
        return create(predicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link BiConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the consumer
     * @param <U> The second input type of the consumer
     * @return The created bi consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U> @NonNull BiConsumer<T, U> createBiConsumer(@NonNull MethodHandle methodHandle) {
        return create(biConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Consumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the consumer
     * @return The created consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull Consumer<T> createConsumer(@NonNull MethodHandle methodHandle) {
        return create(consumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Supplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The result type of the supplier
     * @return The created supplier
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull Supplier<T> createSupplier(@NonNull MethodHandle methodHandle) {
        return create(supplierInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link IntSupplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int supplier
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntSupplier createIntSupplier(@NonNull MethodHandle methodHandle) {
        return create(intSupplierInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link BiFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @param <R> The result type of the function
     * @return The created bi function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U, R> @NonNull BiFunction<T, U, R> createBiFunction(@NonNull MethodHandle methodHandle) {
        return create(biFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Function} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the function
     * @param <R> The result type of the function
     * @return The created function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, R> @NonNull Function<T, R> createFunction(@NonNull MethodHandle methodHandle) {
        return create(functionInterface, methodHandle);
    }

    /**
     * Attempts to create a lambda for the given {@link MethodHandle}
     * implementing the {@link LambdaType}.
     *
     * @param lambdaType The lambda type to implement
     * @param methodHandle The method handle that will be executed by the functional interface
     * @param <T> The functional interface type
     * @return The constructed function
     */
    public static <T> @NonNull T create(@NonNull LambdaType<T> lambdaType, @NonNull MethodHandle methodHandle) {
        return InternalLambdaFactory.create(lambdaType, methodHandle);
    }

    private LambdaFactory() {
    }
}
