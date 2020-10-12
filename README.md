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

### Retain Heap Memory

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

### Retain Metaspace

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