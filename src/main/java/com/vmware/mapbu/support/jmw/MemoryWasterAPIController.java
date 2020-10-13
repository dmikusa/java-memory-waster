package com.vmware.mapbu.support.jmw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

@Controller
@RequestMapping("/api/v1")
public class MemoryWasterAPIController {
	private static final Logger log = LoggerFactory.getLogger(MemoryWasterAPIController.class);

	private List<int[]> allJunk = new ArrayList<>();
	private List<Object> allObjs = new ArrayList<>();
	private List<Thread> allThreads = new ArrayList<>();
	private List<Thread> philosophers = new ArrayList<>();

	@PostMapping("/memory/heap")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public void heap(@RequestParam(defaultValue = "0") int howMuch,
			@RequestParam(defaultValue = "false") boolean retain) {
		if (howMuch <= 0) {
			throw new IllegalArgumentException("You must have a postive number for the amount of junk");
		}
		List<int[]> junk = (retain) ? allJunk : new ArrayList<>();
		log.info("Going to create " + howMuch + " junk of int[1000]...");
		for (int i = 0; i < howMuch; i++) {
			junk.add(new int[1000]);
		}
		log.info("Finished creating " + junk.size() + " pieces of junk. Will these be retained? "
				+ ((retain) ? "yes" : "no"));
	}

	@PostMapping("/memory/metaspace")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public void metaspace(@RequestParam(defaultValue = "0") int howMuch,
			@RequestParam(defaultValue = "false") boolean retain) throws Exception {
		if (howMuch <= 0) {
			throw new IllegalArgumentException("You must have a postive number for the amount of junk");
		}

		List<Object> objs = (retain) ? allObjs : new ArrayList<>();

		log.info("Going to create " + howMuch + " junk classes...");

		for (int i = 0; i < howMuch; i++) {
			// @formatter:off
			Class<?> cp = new ByteBuddy()
				.subclass(Object.class)
				.method(ElementMatchers.isToString())
				.intercept(FixedValue.value("Hello world : " + i))
				.make()
				.load(getClass().getClassLoader())
				.getLoaded();
			// @formatter:on

			// make an instance & old the reference so the class stays in memory
			objs.add(cp.getDeclaredConstructor().newInstance());
		}
		log.info("Finished creating " + objs.size() + " classes. Will these be retained? " + ((retain) ? "yes" : "no"));
	}

	@PostMapping("/threads")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public void threads(@RequestParam(defaultValue = "0") int howMuch, @RequestParam(defaultValue = "1000") int depth) {
		if (howMuch <= 0) {
			throw new IllegalArgumentException("You must have a postive number for the amount of junk");
		}

		List<Thread> threads = new ArrayList<>();
		log.info("Going to create " + howMuch + " junk threads...");
		for (int i = 0; i < howMuch; i++) {
			threads.add(new Thread((Runnable) () -> {
				new StackWaster().go(0, depth);
			}, "junk-thread-" + UUID.randomUUID().toString()));
		}
		threads.stream().forEach(t -> t.start());
		allThreads.addAll(threads);
		log.info("Finished creating " + threads.size() + " threads.");
	}

	@PostMapping("/deadlock")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public void deadlock() {
		log.info("Initiating deadlock, unleashing the Philosophers...");

		Lock[] locks = new Lock[] { new ReentrantLock(), new ReentrantLock(), new ReentrantLock(), new ReentrantLock(),
				new ReentrantLock() };

		if (philosophers.size() > 0) {
			clear_dishes();
		}

		philosophers.add(new Philosopher(locks[0], locks[1]));
		philosophers.add(new Philosopher(locks[1], locks[2]));
		philosophers.add(new Philosopher(locks[2], locks[3]));
		philosophers.add(new Philosopher(locks[3], locks[4]));
		philosophers.add(new Philosopher(locks[4], locks[0]));

		philosophers.stream().forEach(t -> t.start());
	}

	@DeleteMapping("/deadlock")
	@ResponseStatus(code = HttpStatus.OK)
	public void stop_deadlock() {
		clear_dishes();
	}

	private void clear_dishes() {
		log.info("Cleaning up after the Philosophers");
		philosophers.stream().forEach(t -> t.interrupt());
		philosophers.clear();
	}

	private static class StackWaster {
		public void go(int cnt, int depth) {
			if (cnt < depth) {
				this.go(cnt + 1, depth);
			}
			LockSupport.park();
		}
	}

	@PostMapping("/memory/gc")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public void gc() {
		log.info("GC requested");
		System.gc();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	public ResponseEntity<?> illegalArguments(Throwable ex) {
		return new ResponseEntity<>(new IllegalArgumentError(ex.getMessage()), HttpStatus.BAD_REQUEST);
	}

	private class IllegalArgumentError {
		private String error;

		public IllegalArgumentError(String error) {
			this.setError(error);
		}

		@SuppressWarnings("unused")
		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}
	}
}
