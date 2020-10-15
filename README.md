# Java Memory Waster

This is a small project with some endpoints that can be run to generate various scenarios that will consume & waste application memory.

You wouldn't normally do any of this, but it makes for a good app to demonstrate troubleshooting technicques, profiling tools and performance tuning techniques.

## To Build

Just run `./mvnw package`.

## To Run

After building, run `java -jar target/java-memory-waster-0.0.1-SNAPSHOT.jar`.

## To Develop

To develop, including hot reloading, run `./mvnw spring-boot:run`.

## Suggested Activities

Run the app then access [http://localhost:8080](http://localhost:8080) in your browser.

Here are some fun activites you can try.

### Heap Memory

The first box allows you to create a bunch of objects on the heap. 

Enter a value for how many items to create. This is not a memory size, but how large of an array to create. Then check the box to "retain" if you want the app to retain a reference to the objects forever. If not checked, it will just retain them for the duration of the API call.

Here's how to use this to do some fun stuff:

1. A short term memory spike.
    1. Set the junk size to something large, like 10k (this is relative to the size of the heap you define for the app).
    2. Do not check retain.
    3. Click the "Execute" button.

        The net result here is that you've created a bunch of objects onto the heap. If you set the size very large, you'll force an OOME because you won't be able to allocate all of the objects. If the size is smaller and there's enough memory to allocate all of the objects, then you're creating a bunch of garbage which needs to be collected.

        Neither condition are dire, but you'll want to watch out for cases in the application where a user could potentially impact how many objects are created. If that is possible, it gives a user a vector to cause a DoS against the application (by causing a large enough request that the app tries to create too many objects and gets an OOME). 
    
        A common example of this is where a user can send a request which causes the app to pull back records from a database. If the app were to load all of those into memory and put no maximum limit on the size of the result set, then the user could trigger an OOME (which is basically a DoS) by finding a request that would cause a large number of records to be returned.

        The other apsect of this is that even if a user cannot trigger an OOME, it's still possible that the user could cause degraded performance by sending enough requests, which create enough objects, which are only used short-term, which turn into junk and then need to be GC'd. GC is not free, so if too much garbage is created that can put a higher CPU tax on GC.

2. A memory leak.
    1. Set the junk size to something reasonable, like 1k (this is relative to the size of the heap you define for the app).
    2. Check retain.
    3. Click the "Execute" button.

        The net result here is that you've created a bunch of objects on the heap and have kept a reference to them. This prevents garbage collection of the objects, even if the app never needs them again.

        This is a common problem in Java applications, unintentionally retaining a reference to objects which are no longer used. In small doses, it's not a big deal but over time or in long running applications usage will build up and eventually cause an OOME. Tracking the increase and determining what is causing the references to be retained is the only way to resolve this type of issue.

### Metaspace

The second box allows you to create a bunch of class objects dynamically, which raises the Metaspace usage. 

Enter a value for how many unique classes to create. This is not a memory size, but how many dynamic classes will be created. Then check the box to "retain" if you want the app to retain a reference to the class objects forever. If not checked, it will just retain them for the duration of the API call.

Here's how to use this to do some fun stuff:

1. Force load/unload of classes.
    1. Set the junk size to something like 100. You don't need a ton.
    2. Do not check retain.
    3. Click the "Execute" button.

        The net result of this is that the app will go off and dynamically create a bunch of new classes. These will get loaded and increase the metaspace size. Because we're not retaining a reference to these classes, they're no longer needed and the JVM will GC and unload them from metaspace (or unload if you fill up metaspace). This is interesting behavior to watch with a profiler tool, as you can see the metaspace size go up and down. 
    
        You can also search through the class list in your profiler for `net.bytebuddy.renamed` and that will pull back all the dynamically generated classes.

        Another interesting way to view this is to run with `java -Xlog:class+load -Xlog:class+unload -jar target/java-memory-waster-0.0.1-SNAPSHOT.jar`. This will print when classes are loaded and unloaded (*Note* doesn't seem to work when run through Maven).

        In a real application, you wouldn't likely see a ton of classes loaded/unloaded all at once, but this shows how you can track class loading/unloading and what it might look like.

2. Force loading of classes and block the ability to unload.
    1. Set the junk size to something like 100. You don't need a ton.
    2. Check retain.
    3. Click the "Execute" button.
  
        The net result of this is that the app will go off and dynamically create a bunch of new classes. These will get loaded and increase the metaspace size. Because we're retaining a reference to an instance of these classes, they'll remain active and the JVM will not be able to GC the objects and thus not able to unload the classes from metaspace. 
  
        This is interesting behavior to watch with a profiler tool, as you can see the metaspace size go up and never come down. You'll also eventually see an OOME indicating that metaspace filled up.

        This is more realistic than the first task in this section, as most real applications are going to retain instances of a class in memory and thus the class will not be able to be unloaded. Eventually, if enough classes are loaded you'll just run out of metaspace. Typically the fix is to restart and give yourself more metaspace, as you likely just need more metaspace. 
  
        If you suspect a leak, then you'd need to take some heap dumps and look for instances of the class or classes that you believe should have been unloaded. When you find what classes are still alive, then you can figure out what's referring to them. Lastly, you'd need to update the code to not hold the references. Once the references are gone, GC will occur and classes should propertly unload.

### Threads

The third box allows you to create a bunch of threads. Simply enter a value for how many unique threads to create and press the button.

Here's how to use this to do some fun stuff:

1. Force a bunch of threads to be created.
    1. Set the number of threads like 100.
    2. Click the "Execute" button.

        The net result will be a bunch of extra threads hanging around in the application. These threads are not doing anything so they are just parked, however they do consume thread stack memory.

        Connect to the application with your Profiler and click on the tab to view Threads. You can filter on `junk-thread`, which is the prefix given to the threads the app creates. Pick one to look at and you'll see it's just sitting and waiting, doing nothing. *Note* your profiler may list this as a "deadlock" but it's not, the threads are just parked not doing anything which is why the stack isn't changing.

        The next fun thing to do is to enable Java NMT (Native Memory Tracing). This is really the only want to get a good picture of the memory overhead of the threads being created. To do that, we're going to run `java -XX:NativeMemoryTracking=summary -jar target/java-memory-waster-0.0.1-SNAPSHOT.jar`, which enables Java NMT. Now, run `jps` and find the process id of the application. Then run `jcmd <pid> VM.native_memory baseline`. This will take a Java NMT baseline.
        
        Go into the application and create some threads. Then run `jcmd <pid> VM.native_memory summary.diff`. This will output a differential from the baseline that we previously took. Look at the section for threads.

        ```
        -            Thread (reserved=65825KB +20573KB, committed=65825KB +20573KB)
                            (thread #64 +20)
                            (stack: reserved=65536KB +20480KB, committed=65536KB +20480KB)
                            (malloc=216KB +69KB #386 +120)
                            (arena=73KB +23 #126 +40)
        ```

        You can see how the number of threads is at 64 and that it's +20 from the baseline (I created 20 threads in the example, it'll be plus however many you created). We can also see that the stack is up 20480KB or 20MB (i.e 1MB per thread is the default stack size. Homework: try setting `-Xss228k` and repeating the test, how does that impact memory usage?).

### Deadlocks

The fourth box allows you to create a deadlock. Just click the Execute button to initiate this test, and then watch the output logs from the application.

As the deadlock test runs, you'll see the Dining Philosophers attempt to think and eat at the same time. Eventually this will cause problems and they will get into a deadlock. At this point, output from the Philosophers will stop.

Here's how to use this to do some fun stuff:

1. Initiate a deadlock and wait for it to occur.
    1. Click the "Execute" button.
    2. Watch the logs for output.

        The net result will be that there are five threads with a name where the prefix is `Philosopher-`. They will all be blocked waiting on a monitor that is held by one of the other Philosopher threads.

        When the system enters this state, there are a few ways to introspect what has happened.

        First, take a thread dump. Run `jps` and find the process id then run either `kill -3 <pid>` or `jstack <pid>` (both produce the same output).

        The output of the thread dump will indicate that there is a deadlock and you'll see a section like this:

        ```
        Found one Java-level deadlock:
        =============================
        ...
        ```

        This section is important and will show you which threads are blocked and on which monitors they are blocked. Below that is a section that will also contain the stack traces of the deadlocked threads. It looks like this.

        ```
        Java stack information for the threads listed above:
        ===================================================
        ...
        ```

        This is also helpful in debugging and figuring out what caused the deadlock.

        The other way to debug this type of situation is to connect with a Profiler. Any decent profiler will have a section to display the current threads and will also detect the deadlock.

        When you're done, click the Interrupt button and it will clear out the deadlock & philosopher threads.

### Slow Threads

Something more likely to be observed in production than a deadlock is the case where thread progress is slowed down due to a shared resource. You have many threads competing for a limit resource, which causes some threads to block while waiting to get access to the shared resource. 

This is something that's more likely to come up in real applications, and a common example where you'll see this is with backend services. If the backend service is slow, using connections to the backend service will be slow. That can cause connections to be checked out of a resource pool for longer durations and that can cause threads to exhaust the resource pool. Once the pool is exhausted, threads will hang or block on the resource pool waiting for a connection to free up. If this doesn't happen or doesn't happen quickly enough, you may even start to see other failures like timeouts and request failures.

Here's how to use this to do some fun stuff:

1. Initiate a slow threads test.
    1. Click the "Execute" button.
    2. Watch the logs for output.

        The net result of this test will be that 20 threads are launched, all of which are trying to get fake resources from a pool. The pool limits a maximum of 5 fake resources to be checked out at any time which means at any given time there are typically 15 threads waiting. This slows the threads down, just like in a web application if the request processing threads were blocking while trying to get a connection from a pool for a service.

        For clarity, the following information is embedded into the logs.

        ```
        Obtaining permit (available -> 5 -- in use -> 0 -- waiting -> 0)
        Acquired permit (available -> 4 -- in use -> 1 -- waiting -> 0)
        Returned permit (available -> 1 -- in use -> 4 -- waiting -> 15)
        ```

        The first line tells us that a thread is trying to obtain a resource. It tells us how many are available, in use and how many threads are waiting. In this case, all five are available and none are in use. The second line is similar, but just prints after a permit has been acquired. The third line is what you see when a permit is returned to the pool.

        A good resource pool will display this type of information in the logs, metrics or through JMX so that you can monitor and tune its configuration.

        ```
        Permit obtained (2) which took PT4.01145S and has been obtained 7 times.
        ```

        This line tells us which fake data item was obtained, #4, and how long it took, 4.01s, and how many times this thread has managed to obtain a permit, 7. 
        
        As you can see from the logs, the threads can wait quite a long time before obtaining a lock. If this were a web application block on a database connection, the application would be unusably slow and reasonable next steps would be to a.) tune the pool size and b.) investigate why database connections are slow.

        In an actual application, the log information may not be this detailed. In some cases, you can increase log output by lowering the log level of your pool to DEBUG, or something similar. In other cases, you might need to connect via JMX or through some other metrics interface.

        What you can also do is to take thread dumps. Take a few thread dumps, separated by 10-15s. Then review the threads to see what they are doing. Do this now, issue `kill -3 <pid>` or `jstack <pid>` while the test is running.

        You'll see roughly 15 threads in this state:

        ```
        "PoolThread-0" #59 daemon prio=5 os_prio=31 cpu=4.65ms elapsed=45.51s tid=0x00007fcbd02cb800 nid=0x13407 waiting on condition  [0x000070000d70a000]
        java.lang.Thread.State: WAITING (parking)
        at jdk.internal.misc.Unsafe.park(java.base@11.0.8/Native Method)
        - parking to wait for  <0x000000060f27a068> (a java.util.concurrent.Semaphore$NonfairSync)
        at java.util.concurrent.locks.LockSupport.park(java.base@11.0.8/LockSupport.java:194)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(java.base@11.0.8/AbstractQueuedSynchronizer.java:885)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(java.base@11.0.8/AbstractQueuedSynchronizer.java:1039)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(java.base@11.0.8/AbstractQueuedSynchronizer.java:1345)
        at java.util.concurrent.Semaphore.acquire(java.base@11.0.8/Semaphore.java:318)
        at com.vmware.mapbu.support.jmw.SimplePool.obtainPermit(SimplePool.java:32)
        at com.vmware.mapbu.support.jmw.MemoryWasterAPIController.lambda$4(MemoryWasterAPIController.java:164)
        at com.vmware.mapbu.support.jmw.MemoryWasterAPIController$$Lambda$733/0x00000008004d0440.run(Unknown Source)
        at java.lang.Thread.run(java.base@11.0.8/Thread.java:834)
        ```

        In the thread dump, we can see that we're waiting and the line of code that's triggering us to block. This is useful in tracking down the culprit. In most cases, you'll end up seeing it block on obtaining a resource from the pool, which is what's happening here.

        You can also diff multiple thread dumps. The benefit of this is that you can see if threads are making progress. So if you take two thread dumps and waited 15s between the first and second, you can then look at `PoolThread-0` in both and see what it's doing. If it's stuck at the same line of code, then it either hasn't moved for 15s or it's stuck in a hotspot (i.e. the code frequently executes that line).

        As usual, a profiler is also helpful. It can be used to debug thread issues as well. In this case, the profiler will allow you to look at and take thread dumps.

        When you're done, press the Interrupt button and that will end the test.
