package com.escapehub.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Η αρχική σελίδα του UI. Χωρίς αναγνωρισμένο παίκτη ανακατευθύνει στη σελίδα ονόματος.
 */
@Controller
class HomeController {

	@GetMapping("/")
	String home() {
		return "index";
	}

}
