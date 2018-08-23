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

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A factory to create lambda functions from a given {@link MethodHandle}.
 */
@SuppressWarnings("unchecked")
public final class LmbdaFactory {

    // Supplier

    private static final LmbdaType<Supplier> supplierInterface = LmbdaType.of(Supplier.class);

    // Functions

    private static final LmbdaType<Function> functionInterface = LmbdaType.of(Function.class);
    private static final LmbdaType<BiFunction> biFunctionInterface = LmbdaType.of(BiFunction.class);

    // Consumers

    private static final LmbdaType<Consumer> consumerInterface = LmbdaType.of(Consumer.class);
    private static final LmbdaType<BiConsumer> biConsumerInterface = LmbdaType.of(BiConsumer.class);

    // Predicates

    private static final LmbdaType<Predicate> predicateInterface = LmbdaType.of(Predicate.class);
    private static final LmbdaType<BiPredicate> biPredicateInterface = LmbdaType.of(BiPredicate.class);

    /**
     * Attempts to create a {@link Predicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the predicate
     * @param <U> The second input type of the predicate
     * @return The created bi predicate
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T, U> BiPredicate<T, U> createBiPredicate(MethodHandle methodHandle) {
        return create(biPredicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Predicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the predicate
     * @return The created predicate
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T> Predicate<T> createPredicate(MethodHandle methodHandle) {
        return create(predicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link BiConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the consumer
     * @param <U> The second input type of the consumer
     * @return The created bi consumer
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T, U> BiConsumer<T, U> createBiConsumer(MethodHandle methodHandle) {
        return create(biConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Consumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the consumer
     * @return The created consumer
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T> Consumer<T> createConsumer(MethodHandle methodHandle) {
        return create(consumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Supplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The result type of the supplier
     * @return The created supplier
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T> Supplier<T> createSupplier(MethodHandle methodHandle) {
        return create(supplierInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link BiFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @param <R> The result type of the function
     * @return The created bi function
     * @see #create(LmbdaType, MethodHandle)
     */
    public static <T, U, R> BiFunction<T, U, R> createBiFunction(MethodHandle methodHandle) {
        return create(biFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link Function} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the function
     * @param <R> The result type of the function
     * @return The created function
     * @see #create(LmbdaType, MethodHandle), MethodHandle)
     */
    public static <T, R> Function<T, R> createFunction(MethodHandle methodHandle) {
        return create(functionInterface, methodHandle);
    }

    /**
     * Attempts to create a lambda for the given {@link MethodHandle}
     * implementing the {@link LmbdaType}.
     *
     * @param lmbdaType The lmbda type to implement
     * @param methodHandle The method handle that will be executed by the functional interface
     * @param <T> The functional interface type
     * @return The constructed function
     */
    public static <T> T create(LmbdaType<T> lmbdaType, MethodHandle methodHandle) {
        return InternalLambdaFactory.create(lmbdaType, methodHandle);
    }

    private LmbdaFactory() {
    }
}
