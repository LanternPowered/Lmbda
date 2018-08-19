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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class LambdaFactory {

    // Supplier

    private static final FunctionalInterface<Supplier> supplierInterface = FunctionalInterface.of(Supplier.class);

    // Functions

    private static final FunctionalInterface<Function> functionInterface = FunctionalInterface.of(Function.class);
    private static final FunctionalInterface<BiFunction> biFunctionInterface = FunctionalInterface.of(BiFunction.class);

    // Consumers

    private static final FunctionalInterface<Consumer> consumerInterface = FunctionalInterface.of(Consumer.class);
    private static final FunctionalInterface<BiConsumer> biConsumerInterface = FunctionalInterface.of(BiConsumer.class);

    // Predicates

    private static final FunctionalInterface<Predicate> predicateInterface = FunctionalInterface.of(Predicate.class);
    private static final FunctionalInterface<BiPredicate> biPredicateInterface = FunctionalInterface.of(BiPredicate.class);

    /**
     * Attempts to create a {@link Predicate} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The first input type of the predicate
     * @param <U> The second input type of the predicate
     * @return The created bi predicate
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T, U> BiPredicate<T, U> createBiPredicate(Executable executable) {
        return create(biPredicateInterface, executable);
    }

    /**
     * Attempts to create a {@link Predicate} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The input type of the predicate
     * @return The created predicate
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T> Predicate<T> createPredicate(Executable executable) {
        return create(predicateInterface, executable);
    }

    /**
     * Attempts to create a {@link BiConsumer} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The first input type of the consumer
     * @param <U> The second input type of the consumer
     * @return The created bi consumer
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T, U> BiConsumer<T, U> createBiConsumer(Executable executable) {
        return create(biConsumerInterface, executable);
    }

    /**
     * Attempts to create a {@link Consumer} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The input type of the consumer
     * @return The created consumer
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T> Consumer<T> createConsumer(Executable executable) {
        return create(consumerInterface, executable);
    }

    /**
     * Attempts to create a {@link Supplier} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The result type of the supplier
     * @return The created supplier
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T> Supplier<T> createSupplier(Executable executable) {
        return create(supplierInterface, executable);
    }

    /**
     * Attempts to create a {@link BiFunction} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The first input type of the function
     * @param <U> The second input type of the function
     * @param <R> The result type of the function
     * @return The created bi function
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T, U, R> BiFunction<T, U, R> createBiFunction(Executable executable) {
        return create(biFunctionInterface, executable);
    }

    /**
     * Attempts to create a {@link Function} for the given {@link Executable}.
     *
     * @param executable The executable
     * @param <T> The input type of the function
     * @param <R> The result type of the function
     * @return The created function
     * @see #create(FunctionalInterface, Executable)
     */
    public static <T, R> Function<T, R> createFunction(Executable executable) {
        return create(functionInterface, executable);
    }

    /**
     * Attempts to create a lambda for the given {@link Executable}
     * implementing the {@link FunctionalInterface}.
     *
     * <p>The provided {@link Executable} must be accessible before
     * the functional interface can be implemented.</p>
     *
     * @param functionalInterface The functional interface to implement
     * @param executable The executable to call
     * @param <T> The functional interface type
     * @param <F> The function type
     * @return The function
     */
    @SuppressWarnings("unchecked")
    public static <T, F extends T> F create(FunctionalInterface<T> functionalInterface, Executable executable) {
        try {
            // The trusted lookup can be used here, because the privileges are already
            // required to make the executable accessible. This unreflect methods will
            // fail if this isn't the case.

            final MethodHandles.Lookup lookup = UnsafeMethodHandles.trustedLookup.in(executable.getDeclaringClass());
            final MethodHandle methodHandle;
            if (executable instanceof Constructor) {
                methodHandle = lookup.unreflectConstructor((Constructor<?>) executable);
            } else {
                methodHandle = lookup.unreflect((Method) executable);
            }

            // Generate the lambda class
            final CallSite callSite = LambdaMetafactory.metafactory(lookup, functionalInterface.methodName,
                    functionalInterface.classType, functionalInterface.methodType, methodHandle, methodHandle.type());

            // Create the function
            return (F) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't create lambda for: \"" + executable + "\". "
                    + "Failed to implement: " + functionalInterface, e);
        }
    }

    /**
     * Attempts to create a lambda for the given {@link MethodHandle}
     * implementing the {@link FunctionalInterface}.
     *
     * @param functionalInterface The functional interface to implement
     * @param methodHandle The method handle that will be executed by the functional interface
     * @param <T> The functional interface type
     * @param <F> The function type
     * @return The function
     */
    @SuppressWarnings("unchecked")
    public static <T, F extends T> F create(FunctionalInterface<T> functionalInterface, MethodHandle methodHandle) {
        try {
            final MethodHandleInfo info = UnsafeMethodHandles.trustedLookup.revealDirect(methodHandle);
            final MethodHandles.Lookup lookup = UnsafeMethodHandles.trustedLookup.in(info.getDeclaringClass());

            // Generate the lambda class
            final CallSite callSite = LambdaMetafactory.metafactory(lookup, functionalInterface.methodName,
                    functionalInterface.classType, functionalInterface.methodType, methodHandle, methodHandle.type());

            // Create the function
            return (F) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't create lambda for: \"" + methodHandle + "\". "
                    + "Failed to implement: " + functionalInterface, e);
        }
    }

    private LambdaFactory() {
    }
}
