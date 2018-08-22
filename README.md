# Lmbda [![Build Status](https://travis-ci.org/LanternPowered/Lmbda.svg?branch=master)](https://travis-ci.org/LanternPowered/Lmbda) [![Discord](https://img.shields.io/badge/chat-on%20discord-6E85CF.svg)](https://discord.gg/ArSrsuU)

This is library that can be used to generate lambdas from methods, constructors, field getters and setters.

* [Source]
* [Issues]
* [Wiki]

[Source]: https://github.com/LanternPowered/Lmbda
[Issues]: https://github.com/LanternPowered/Lmbda/issues
[Wiki]: https://github.com/LanternPowered/Lmbda/wiki

## How to use

### Generating method functions

Consider the following class which holds a static setter function.
```java
class MyObject {
    private static void set(int value) {
        // ...
    }
}
```
If you want to generate a function for the `set(int)` method, you need to use
a interface which takes one parameter (`value`) and return `void` to reflect
the method signature. The `IntConsumer` is a good choice available within the
JRE.

```java
class MyClass {
    
    // The interface that will be implemented, can be cached
    private static final FunctionalInterface<IntConsumer> intConsumerInterface = FunctionalInterface.of(IntConsumer.class);
        
    void test() throws Exception {
        final MethodHandles.Lookup lookup = MethodHandlesX.privateLookupIn(MyObject.class, MethodHandles.lookup());
        final MethodHandle methodHandle = lookup.findStatic(MyObject.class, "set", MethodType.methodType(void.class, int.class));
        
        // Create the setter function, the lookup needs to have access to the given method handle
        final IntConsumer setter = LambdaFactory.create(intConsumerInterface, lookup, methodHandle);
        setter.accept(1000);
    }
}
```

Note: Generating functions requires that you have access to the target class.
In java 9, this is even more important, see `MethodHandles(X).privateLookupIn`.

## Benchmarks

Below you can find a few benchmarks. `lmbda` will be generated at runtime, all the other
ones are implemented manually. The generated structure of the `lmbda` is almost the
same as the `static_mh`.

The following benchmark implements a lambda function to access a `int` field, a `ToIntFunction`
is implemented in this case to avoid boxing/unboxing.

Benchmark                                        | Mode | Cnt | Score | Error | Units
-------------------------------------------------|:----:|:---:|:-----:|:-----:|:----:
LambdaFactoryIntGetterBenchmark.dynamic_mh       | avgt | 15 | 3.819 | ± 0.112 | ns/op
LambdaFactoryIntGetterBenchmark.dynamic_reflect  | avgt | 15 | 4.663 | ± 0.008 | ns/op
LambdaFactoryIntGetterBenchmark.lmbda            | avgt | 15 | 1.930 | ± 0.006 | ns/op
LambdaFactoryIntGetterBenchmark.plain            | avgt | 15 | 1.926 | ± 0.003 | ns/op
LambdaFactoryIntGetterBenchmark.static_mh        | avgt | 15 | 1.927 | ± 0.003 | ns/op
LambdaFactoryIntGetterBenchmark.static_reflect   | avgt | 15 | 3.918 | ± 0.005 | ns/op
