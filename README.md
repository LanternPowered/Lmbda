## Lmbda [![Discord](https://img.shields.io/badge/chat-on%20discord-6E85CF.svg)](https://discord.gg/ArSrsuU) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.lanternpowered/lmbda/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.lanternpowered/lmbda)

This is library that can be used to generate lambdas from method handles. This includes methods, constructors, field accessors (getter, setter, even for final fields) and any other [`MethodHandle`] that can be constructed.

* [Source]
* [Issues]
* [Wiki]

[Source]: https://github.com/LanternPowered/Lmbda
[Issues]: https://github.com/LanternPowered/Lmbda/issues
[Wiki]: https://github.com/LanternPowered/Lmbda/wiki

[`MethodHandle`]: https://docs.oracle.com/javase/10/docs/api/java/lang/invoke/MethodHandle.html
[`MethodHandles#privateLookupIn`]: https://docs.oracle.com/javase/10/docs/api/java/lang/invoke/MethodHandles.html#privateLookupIn(java.lang.Class,java.lang.invoke.MethodHandles.Lookup)

## How to use

Every available `MethodHandle` can be implemented by a functional interface. But only if the two method signatures match. Object types will be auto casted to the target type if possible, the same goes for boxing/unboxing of primitive types.

Non static methods will always take a extra parameter which represents the target object of the method. This is always the first parameter.

Note: Generating functions requires that you have access to the target class.
Since Java 9, this is even more important because the module your code is located in must be able to access the module of the target class, see
 [`MethodHandles#privateLookupIn`] for more info.

### Generating method functions

#### Static setter method

Consider the following class which holds a static setter function.
```java
class MyObject {
    private static void set(int value) {
        // ...
    }
}
```
If you want to generate a function for the static `set(int)` method, you need to use
a interface which takes one parameter (`value`) and return `void` to reflect
the method signature. The `IntConsumer` is a good choice available within the
Java 8 API.

```java
final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(MyObject.class, MethodHandles.lookup());
final MethodHandle methodHandle = lookup.findStatic(MyObject.class, "set", MethodType.methodType(void.class, int.class));

final IntConsumer setter = LambdaFactory.create(LambdaType.of(IntConsumer.class), methodHandle);
setter.accept(1000);
```

#### Setter method

Consider the following class which holds a setter function.
```java
class MyObject {
    private void set(int value) {
        // ...
    }
}
```

If you want to generate a function for the static `set(int)` method, you need to use
a interface which takes two parameters:

1. The target of where the method is declared, this will be the target object on which the method is invoked. E.g. `target.set(value)`
2. The value that will be passed to the `set` method.

the first one is the target where the method is declared, `MyObject` (`value`) and return `void` to reflect
the method signature. The `ObjIntConsumer` is a good choice available within the
Java 8 API.

```java
final MethodHandles.Lookup lookup = MethodHandlesExtensions.privateLookupIn(MyObject.class, MethodHandles.lookup());
final MethodHandle methodHandle = lookup.findVirtual(MyObject.class, "set", MethodType.methodType(void.class, int.class));

final ObjIntConsumer<MyObject> setter = LambdaFactory.create(new LambdaType<ObjIntConsumer<MyObject>>() {}, methodHandle);

final MyObject myObject = new MyObject();
setter.accept(myObject, 1000);
```


## Benchmarks

Below you can find a few benchmarks. `lmbda` will be generated at runtime, all the other
ones are implemented manually. The generated structure of the `lmbda` is almost the
same as the `static_mh`.

The following benchmark implements a lambda function to access a `int` field, a `ToIntFunction`
is implemented in this case to avoid boxing/unboxing.

Benchmark                                        | Mode | Cnt | Score | Error | Units
-------------------------------------------------|:----:|:---:|:-----:|:-----:|:----:
IntGetterFieldBenchmark.dynamic_mh               | avgt | 15 | 3.891 | ± 0.450 | ns/op
IntGetterFieldBenchmark.dynamic_reflect          | avgt | 15 | 4.788 | ± 0.205 | ns/op
IntGetterFieldBenchmark.lmbda                    | avgt | 15 | 1.988 | ± 0.061 | ns/op
IntGetterFieldBenchmark.plain                    | avgt | 15 | 1.969 | ± 0.049 | ns/op
IntGetterFieldBenchmark.static_mh                | avgt | 15 | 1.935 | ± 0.021 | ns/op
IntGetterFieldBenchmark.static_reflect           | avgt | 15 | 3.912 | ± 0.007 | ns/op


This benchmark returns a `Integer` through the `getValue` method from a target object.

Benchmark                                        | Mode | Cnt | Score | Error | Units
-------------------------------------------------|:----:|:---:|:-----:|:-----:|:----:
IntGetterMethodBenchmark.dynamic_mh              | avgt | 15 |  3.986 | ± 0.088 | ns/op
IntGetterMethodBenchmark.dynamic_reflect         | avgt | 15 |  4.524 | ± 0.028 | ns/op
IntGetterMethodBenchmark.lambda                  | avgt | 15 |  1.986 | ± 0.030 | ns/op
IntGetterMethodBenchmark.lmbda                   | avgt | 15 |  1.977 | ± 0.007 | ns/op
IntGetterMethodBenchmark.plain                   | avgt | 15 |  2.032 | ± 0.007 | ns/op
IntGetterMethodBenchmark.proxy                   | avgt | 15 | 10.254 | ± 0.196 | ns/op
IntGetterMethodBenchmark.static_mh               | avgt | 15 |  2.050 | ± 0.085 | ns/op
IntGetterMethodBenchmark.static_reflect          | avgt | 15 |  4.668 | ± 0.030 | ns/op
