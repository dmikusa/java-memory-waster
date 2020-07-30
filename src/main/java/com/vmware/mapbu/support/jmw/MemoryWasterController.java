package com.vmware.mapbu.support.jmw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

@Controller
public class MemoryWasterController {
	private static final Logger log = LoggerFactory.getLogger(MemoryWasterController.class);

	private List<int[]> allJunk = new ArrayList<>();
	private List<Object> allObjs = new ArrayList<>();
	private List<Thread> allThreads = new ArrayList<>();

	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/heap")
	public String heap(@RequestParam(defaultValue = "0") int howMuch,
			@RequestParam(defaultValue = "false") boolean retain) {
		List<int[]> junk = (retain) ? allJunk : new ArrayList<>();
		log.info("Going to create " + howMuch + " junk of int[1000]...");
		for (int i = 0; i < howMuch; i++) {
			junk.add(new int[1000]);
		}
		log.info("Created " + junk.size() + " pieces of junk.");
		return "redirect:/";
	}

	@RequestMapping("/metaspace")
	public String metaspace(@RequestParam(defaultValue = "0") int howMuch,
			@RequestParam(defaultValue = "false") boolean retain) throws Exception {
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
		log.info("Created " + objs.size() + " classes");
		return "redirect:/";
	}

	@RequestMapping("/threads")
	public String threads(@RequestParam(defaultValue = "0") int howMuch, @RequestParam(defaultValue = "1000") int depth,
			@RequestParam(defaultValue = "false") boolean retain) {
		List<Thread> threads = new ArrayList<>();
		log.info("Going to create " + howMuch + " junk threads...");
		for (int i = 0; i < howMuch; i++) {
			threads.add(new Thread((Runnable) () -> {
				new StackWaster().go(0, depth);
			}, "junk-thread-" + UUID.randomUUID().toString()));
		}
		threads.stream().forEach(t -> t.start());
		if (retain) {
			allThreads.addAll(threads);
		}
		log.info("Created " + threads.size() + " threads");
		return "redirect:/";
	}

	private class StackWaster {
		public void go(int cnt, int depth) {
			if (cnt < depth) {
				this.go(cnt + 1, depth);
			}
			LockSupport.park();
		}
	}

	@RequestMapping("/gc")
	public String gc() {
		System.gc();
		return "redirect:/";
	}
}
