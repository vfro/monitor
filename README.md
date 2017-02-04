# monitor

Implementation of Monitor Concurrency Pattern in Java.

For more information see Project [site](https://vfro.github.io/monitor) or [JavaDoc](https://vfro.github.io/monitor/apidocs/index.html).

Monitor can be used for concurrent access to its value. It also provides an ability to wait until the value becomes into some certain state.

```java
 Monitor<Queue<String>> outputQueue =
    new Monitor<>(new LinkedList<String>());

 // ... monitoring thread
 while(true) {
    // Wait until some string is added to the queue
    // and print it into System.out
    outputQueue.writeAccess(
       queue -> {
             System.out.println(queue.pull());

             // Write access lambda must return the parameter object
             // to preserve reference to the queue
             return queue;
          },

       // Wake up when the queue is not empty
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

# installation from maven

```xml
        <dependency>
            <groupId>com.github.vfro</groupId>
            <artifactId>monitor</artifactId>
            <version>3.0.0</version>
        </dependency>
```
