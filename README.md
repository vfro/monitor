# monitor

Java implementation of [Monitor Concurrency Pattern][1]

## motivation

`Monitor` is a synchronization construct that allows threads to have both
thread-safe access to underlying monitored object or value (hereinafter
monitored entity) and wait for it to mutate (if it is a mutable object)
or change (if it is an immutable value) to a desirable state.

## understanding monitor

`Monitor<Entity>` class allows threads to access monitored entity in either
exclusive (write) or shared (read) mode. `Entity` here refers to type
of monitored entity.

For accessing monitored entity `Monitor` class has several overloaded
`read` and `write` methods.

* all `read` methods take `Consumer<Entity>` as a first argument. It could
be a lambda function that reads monitored entity but must not mutate it. The
lambda function takes monitored entity as its only argument and must return
no value.

* all `write` methods take `Function<Entity, Entity>` as a first argument.
It could be a lambda function that accesses monitored value exclusively.
The lambda function must return a reference to the monitored entity.
This could be the same value that received by the lambda function in its
argument (if the lambda function mutates monitored entity) or a new value
(if the lambda function's intent is to change the reference to monitored
entity).

* `read` and `write` methods might take `Predicate<Entity>` as a second
argument. It could be a lambda function that evaluates monitored entity
and indicates if the desirable state is achieved. Concurrent predicates
examine monitored entity exclusively.

* `read` and `write` methods might take a maximum time to wait as a third
argument. If the time detectably elapsed the methods return false and monitored
entity is not accessed.

### type safety note

Unfortunately, Java type system cannot guarantee through `Monitor`'s contract
that consumers and predicates do not mutate monitored entity. `Monitor`'s
behavior in this case is undefined.

Consumers and predicates must not mutate monitored entity even if it is an
instance of `Atomic` classes (see `java.util.concurrent.atomic`) or synchronized
in some other way. `Monitor` is not able to detect changes in this case and will
not re-evaluate predicates of other waiting threads.

## sandbox

`Sandbox` class allows threads to create a local copy of monitored entity and
then Compare and Swap the mutated or changed entity back to monitor. The
`Entity` must be `Cloneable` in order to create its local copy.

`Sandbox` instance is not reentrant. Each thread must create a local instance
of `Sandbox`.

## javadoc

For more information see Project [website][2] or [JavaDoc][3].

## example

```java
 Monitor<Queue<String>> outputQueue =
    new Monitor<>(new LinkedList<String>());

 // ... monitoring thread
 while(true) {
    // wait for a new string in queue
    outputQueue.writeAccess(
       queue -> {
             System.out.println(queue.pull());

             // Write access lambda must return its argument
             // to preserve reference to the queue
             return queue;
          },

       // wake up when the queue is not empty
       queue -> !queue.isEmpty()
    );
 }

 // ... somewhere in some other thread
 outputQueue.writeAccess(
    queue -> {
          queue.add("Hello Monitor!");
          return queue;
       }
 );
```

## `Monitor` compared to other synchronization constructs

## compared to `synchronized`

`Monitor` is different from Java `synchronized` keyword in several crucial
ways:

* Unlike `synchronized` keyword `Monitor` allows threads to wait for monitored
entity to mutate or change to a desirable state.

* Unlike `synchronized` keyword `Monitor` allows threads to access monitored
entity in either exclusive or shared mode.

## compared to `Object.wait()`

`Object.wait()` method allow threads to wait for a desirable event to occur.
The event occurrence is determined by actor thread and signaled by invoking
`Object.notify()` or `Object.notifyAll()` methods.

Unlike `Object.wait()` method `Monitor` allows threads to wait for desirable
state which is defined by a `Predicate`. The actor thread behavior is decoupled
from waiting threads intentions. 

## compared to `Atomic` classes

* Even though `Monitor` provides an ability to perform Compare and Swap over
monitored entity it cannot be considered lock-free or wait-free construct and
thus it is not recommended for use in lock-free or wait-free algorithms.

* `Atomic` classes could have less overhead and be more effective when used for
short-term non-blocking spin lock or wait constructs. Even though `Monitor` has
an overhead for blocking and resuming threads it could be more effective it case
of mid- or long-term blocks and waits as blocked and waiting threads do not
consume CPU time. 

## dependency in pom.xml

```xml
        <dependency>
            <groupId>com.github.vfro</groupId>
            <artifactId>monitor</artifactId>
            <version>4.0.0-SNAPSHOT</version>
        </dependency>
```

  [1]: https://en.wikipedia.org/wiki/Monitor_(synchronization)
  [2]: https://vfro.github.io/monitor
  [3]: https://vfro.github.io/monitor/apidocs/index.html
