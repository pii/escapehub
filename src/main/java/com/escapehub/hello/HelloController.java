package com.escapehub.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HelloController {

	@GetMapping("/hello")
	Greeting hello(@RequestParam(defaultValue = "world") String name) {
		return new Greeting("Hello, " + name + "!");
	}

	record Greeting(String message) {
	}

}
