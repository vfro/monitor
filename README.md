# monitor

Implementation of Monitor Concurrency Pattern.

For more information see Project [site](https://vfro.github.io/monitor) or [JavaDoc](https://vfro.github.io/monitor/apidocs/index.html).

Monitor can be used for concurrent access to its value. It also provides an ability to wait until the value becomes into some certain state.

```java
 Monitor<Queue<String>> outputQueue =
    new Monitor<>(new LinkedList<String>());

 // ...
 while(true) {
    // Wait until some string is added to a queue
    // and printit into System.out
    outputQueue.writeAccess(
       queue -> {
             System.out.println(queue.pull());

             // Write access lambda must return the parameter object
             // to preserve reference to the queue
             return queue;
          },
       queue -> !queue.isEmpty()
    );
 }

 // Some other thread
 outputQueue.writeAccess(
    queue -> {
          queue.add("Hello Monitor!");
          return queue;
       }
 );
```