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
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

/**
 * A factory to create lambda functions from a given {@link MethodHandle}.
 */
@SuppressWarnings("unchecked")
public final class LambdaFactory {

    // Supplier

    private static final LambdaType<Supplier> supplierInterface = LambdaType.of(Supplier.class);
    private static final LambdaType<IntSupplier> intSupplierInterface = LambdaType.of(IntSupplier.class);
    private static final LambdaType<DoubleSupplier> doubleSupplierInterface = LambdaType.of(DoubleSupplier.class);
    private static final LambdaType<LongSupplier> longSupplierInterface = LambdaType.of(LongSupplier.class);
    private static final LambdaType<BooleanSupplier> booleanSupplierInterface = LambdaType.of(BooleanSupplier.class);

    // Functions

    private static final LambdaType<Function> functionInterface = LambdaType.of(Function.class);
    private static final LambdaType<IntFunction> intFunctionInterface = LambdaType.of(IntFunction.class);
    private static final LambdaType<ToIntFunction> toIntFunctionInterface = LambdaType.of(ToIntFunction.class);
    private static final LambdaType<DoubleFunction> doubleFunctionInterface = LambdaType.of(DoubleFunction.class);
    private static final LambdaType<ToDoubleFunction> toDoubleFunctionInterface = LambdaType.of(ToDoubleFunction.class);
    private static final LambdaType<LongFunction> longFunctionInterface = LambdaType.of(LongFunction.class);
    private static final LambdaType<ToLongFunction> toLongFunctionInterface = LambdaType.of(ToLongFunction.class);
    private static final LambdaType<IntToLongFunction> intToLongFunctionInterface = LambdaType.of(IntToLongFunction.class);
    private static final LambdaType<IntToDoubleFunction> intToDoubleFunctionInterface = LambdaType.of(IntToDoubleFunction.class);
    private static final LambdaType<DoubleToIntFunction> doubleToIntFunctionInterface = LambdaType.of(DoubleToIntFunction.class);
    private static final LambdaType<DoubleToLongFunction> doubleToLongFunctionInterface = LambdaType.of(DoubleToLongFunction.class);
    private static final LambdaType<LongToIntFunction> longToIntFunctionInterface = LambdaType.of(LongToIntFunction.class);
    private static final LambdaType<LongToDoubleFunction> longToDoubleFunctionInterface = LambdaType.of(LongToDoubleFunction.class);

    private static final LambdaType<BiFunction> biFunctionInterface = LambdaType.of(BiFunction.class);
    private static final LambdaType<ToIntBiFunction> toIntBiFunctionInterface = LambdaType.of(ToIntBiFunction.class);
    private static final LambdaType<ToDoubleBiFunction> toDoubleBiFunctionInterface = LambdaType.of(ToDoubleBiFunction.class);
    private static final LambdaType<ToLongBiFunction> toLongBiFunctionInterface = LambdaType.of(ToLongBiFunction.class);

    // Consumers

    private static final LambdaType<Consumer> consumerInterface = LambdaType.of(Consumer.class);
    private static final LambdaType<IntConsumer> intConsumerInterface = LambdaType.of(IntConsumer.class);
    private static final LambdaType<DoubleConsumer> doubleConsumerInterface = LambdaType.of(DoubleConsumer.class);
    private static final LambdaType<LongConsumer> longConsumerInterface = LambdaType.of(LongConsumer.class);

    private static final LambdaType<BiConsumer> biConsumerInterface = LambdaType.of(BiConsumer.class);
    private static final LambdaType<ObjIntConsumer> objIntConsumerInterface = LambdaType.of(ObjIntConsumer.class);
    private static final LambdaType<ObjDoubleConsumer> objDoubleConsumerInterface = LambdaType.of(ObjDoubleConsumer.class);
    private static final LambdaType<ObjLongConsumer> objLongConsumerInterface = LambdaType.of(ObjLongConsumer.class);

    // Predicates

    private static final LambdaType<Predicate> predicateInterface = LambdaType.of(Predicate.class);
    private static final LambdaType<IntPredicate> intPredicateInterface = LambdaType.of(IntPredicate.class);
    private static final LambdaType<DoublePredicate> doublePredicateInterface = LambdaType.of(DoublePredicate.class);
    private static final LambdaType<LongPredicate> longPredicateInterface = LambdaType.of(LongPredicate.class);

    private static final LambdaType<BiPredicate> biPredicateInterface = LambdaType.of(BiPredicate.class);

    // Operator

    private static final LambdaType<BinaryOperator> binaryOperatorInterface = LambdaType.of(BinaryOperator.class);
    private static final LambdaType<IntBinaryOperator> intBinaryOperatorInterface = LambdaType.of(IntBinaryOperator.class);
    private static final LambdaType<DoubleBinaryOperator> doubleBinaryOperatorInterface = LambdaType.of(DoubleBinaryOperator.class);
    private static final LambdaType<LongBinaryOperator> longBinaryOperatorInterface = LambdaType.of(LongBinaryOperator.class);

    private static final LambdaType<UnaryOperator> unaryOperatorInterface = LambdaType.of(UnaryOperator.class);
    private static final LambdaType<IntUnaryOperator> intUnaryOperatorInterface = LambdaType.of(IntUnaryOperator.class);
    private static final LambdaType<DoubleUnaryOperator> doubleUnaryOperatorInterface = LambdaType.of(DoubleUnaryOperator.class);
    private static final LambdaType<LongUnaryOperator> longUnaryOperatorInterface = LambdaType.of(LongUnaryOperator.class);

    /**
     * Attempts to create a {@link BinaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The target type of the binary operator
     * @return The created binary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull BinaryOperator<T> createBinaryOperator(@NonNull MethodHandle methodHandle) {
        return create(binaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link IntBinaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int binary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntBinaryOperator createIntBinaryOperator(@NonNull MethodHandle methodHandle) {
        return create(intBinaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleBinaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double binary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleBinaryOperator createDoubleBinaryOperator(@NonNull MethodHandle methodHandle) {
        return create(doubleBinaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongBinaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long binary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongBinaryOperator createLongBinaryOperator(@NonNull MethodHandle methodHandle) {
        return create(longBinaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link UnaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The target type of the unary operator
     * @return The created unary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull UnaryOperator<T> createUnaryOperator(@NonNull MethodHandle methodHandle) {
        return create(unaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link IntUnaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int unary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntUnaryOperator createIntUnaryOperator(@NonNull MethodHandle methodHandle) {
        return create(intUnaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleUnaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double unary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleUnaryOperator createDoubleUnaryOperator(@NonNull MethodHandle methodHandle) {
        return create(doubleUnaryOperatorInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongUnaryOperator} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long unary operator
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongUnaryOperator createLongUnaryOperator(@NonNull MethodHandle methodHandle) {
        return create(longUnaryOperatorInterface, methodHandle);
    }

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
     * Attempts to create a {@link IntPredicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int predicate
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntPredicate createIntPredicate(@NonNull MethodHandle methodHandle) {
        return create(intPredicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoublePredicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double predicate
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoublePredicate createDoublePredicate(@NonNull MethodHandle methodHandle) {
        return create(doublePredicateInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongPredicate} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long predicate
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongPredicate createLongPredicate(@NonNull MethodHandle methodHandle) {
        return create(longPredicateInterface, methodHandle);
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
     * Attempts to create a {@link ObjIntConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the consumer
     * @return The created obj int consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ObjIntConsumer<T> createObjIntConsumer(@NonNull MethodHandle methodHandle) {
        return create(objIntConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ObjDoubleConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the consumer
     * @return The created obj double consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ObjDoubleConsumer<T> createObjDoubleConsumer(@NonNull MethodHandle methodHandle) {
        return create(objDoubleConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ObjLongConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the consumer
     * @return The created obj long consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ObjLongConsumer<T> createObjLongConsumer(@NonNull MethodHandle methodHandle) {
        return create(objLongConsumerInterface, methodHandle);
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
     * Attempts to create a {@link IntConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntConsumer createIntConsumer(@NonNull MethodHandle methodHandle) {
        return create(intConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleConsumer createDoubleConsumer(@NonNull MethodHandle methodHandle) {
        return create(doubleConsumerInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongConsumer} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long consumer
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongConsumer createLongConsumer(@NonNull MethodHandle methodHandle) {
        return create(longConsumerInterface, methodHandle);
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
     * Attempts to create a {@link DoubleSupplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double supplier
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleSupplier createDoubleSupplier(@NonNull MethodHandle methodHandle) {
        return create(doubleSupplierInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongSupplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long supplier
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongSupplier createLongSupplier(@NonNull MethodHandle methodHandle) {
        return create(longSupplierInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link BooleanSupplier} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created boolean supplier
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull BooleanSupplier createBooleanSupplier(@NonNull MethodHandle methodHandle) {
        return create(booleanSupplierInterface, methodHandle);
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
     * Attempts to create a {@link ToIntBiFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @return The created to int bi function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U> @NonNull ToIntBiFunction<T, U> createToIntBiFunction(@NonNull MethodHandle methodHandle) {
        return create(toIntBiFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ToDoubleBiFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @return The created to double bi function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U> @NonNull ToDoubleBiFunction<T, U> createToDoubleBiFunction(@NonNull MethodHandle methodHandle) {
        return create(toDoubleBiFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ToLongBiFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @return The created to long bi function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T, U> @NonNull ToLongBiFunction<T, U> createToLongBiFunction(@NonNull MethodHandle methodHandle) {
        return create(toLongBiFunctionInterface, methodHandle);
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
     * Attempts to create a {@link IntFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <R> The result type of the function
     * @return The created int function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <R> @NonNull IntFunction<R> createIntFunction(@NonNull MethodHandle methodHandle) {
        return create(intFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ToIntFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the function
     * @return The created to int function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ToIntFunction<T> createToIntFunction(@NonNull MethodHandle methodHandle) {
        return create(toIntFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <R> The result type of the function
     * @return The created double function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <R> @NonNull DoubleFunction<R> createDoubleFunction(@NonNull MethodHandle methodHandle) {
        return create(doubleFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ToDoubleFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the function
     * @return The created to double function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ToDoubleFunction<T> createToDoubleFunction(@NonNull MethodHandle methodHandle) {
        return create(toDoubleFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <R> The result type of the function
     * @return The created long function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <R> @NonNull LongFunction<R> createLongFunction(@NonNull MethodHandle methodHandle) {
        return create(longFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link ToLongFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @param <T> The input type of the function
     * @return The created to long function
     * @see #create(LambdaType, MethodHandle)
     */
    public static <T> @NonNull ToLongFunction<T> createToLongFunction(@NonNull MethodHandle methodHandle) {
        return create(toLongFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link IntToLongFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int to long function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntToLongFunction createIntToLongFunction(@NonNull MethodHandle methodHandle) {
        return create(intToLongFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link IntToDoubleFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created int to double function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull IntToDoubleFunction createIntToDoubleFunction(@NonNull MethodHandle methodHandle) {
        return create(intToDoubleFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleToIntFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double to int function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleToIntFunction createDoubleToIntFunction(@NonNull MethodHandle methodHandle) {
        return create(doubleToIntFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link DoubleToLongFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created double to long function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull DoubleToLongFunction createDoubleToLongFunction(@NonNull MethodHandle methodHandle) {
        return create(doubleToLongFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongToIntFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long to int function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongToIntFunction createLongToIntFunction(@NonNull MethodHandle methodHandle) {
        return create(longToIntFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a {@link LongToDoubleFunction} for the given {@link MethodHandle}.
     *
     * @param methodHandle The method handle
     * @return The created long to double function
     * @see #create(LambdaType, MethodHandle)
     */
    public static @NonNull LongToDoubleFunction createLongToDoubleFunction(@NonNull MethodHandle methodHandle) {
        return create(longToDoubleFunctionInterface, methodHandle);
    }

    /**
     * Attempts to create a lambda for the given {@link MethodHandle}
     * implementing the {@link LambdaType}.
     *
     * <p>This method can also throw a {@link IllegalAccessException} if
     * the default or provided {@link java.lang.invoke.MethodHandles.Lookup}
     * doesn't have proper access to implement the {@link LambdaType}. This
     * exception is thrown as an unchecked exception for convenience.</p>
     *
     * @param lambdaType The lambda type to implement
     * @param methodHandle The method handle that will be executed by the functional interface
     * @param <T> The functional interface type
     * @return The constructed function
     */
    public static <@NonNull T> T create(@NonNull LambdaType<T> lambdaType, @NonNull MethodHandle methodHandle) {
        return InternalLambdaFactory.create(lambdaType, methodHandle);
    }

    private LambdaFactory() {
    }
}
