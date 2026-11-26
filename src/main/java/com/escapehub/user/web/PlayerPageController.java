package com.escapehub.user.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.ConflictException;
import com.escapehub.user.application.PlayerService;
import com.escapehub.user.application.RegisteredPlayer;

/**
 * Η σελίδα όπου ο παίκτης δίνει το όνομά του (US-01).
 */
@Controller
@RequestMapping("/player")
class PlayerPageController {

	/**
	 * Πού πηγαίνει ο παίκτης μετά την εγγραφή. Όταν υπάρξει ο κατάλογος αποστολών (#27), γίνεται
	 * {@code redirect:/missions}.
	 */
	static final String AFTER_REGISTRATION = "redirect:/";

	private static final String VIEW = "player/name";

	private final PlayerService players;

	PlayerPageController(PlayerService players) {
		this.players = players;
	}

	@GetMapping
	String form(Model model) {
		model.addAttribute("form", new NameForm(""));
		return VIEW;
	}

	@PostMapping
	String register(@ModelAttribute("form") NameForm form, Model model, HttpServletRequest request,
			HttpServletResponse response, RedirectAttributes redirect) {
		try {
			RegisteredPlayer player = players.register(form.name());
			response.addHeader(HttpHeaders.SET_COOKIE, PlayerCookie.create(player.token(), request));
			redirect.addFlashAttribute("message", "Καλώς ήρθες, " + player.name() + "!");
			return AFTER_REGISTRATION;
		}
		catch (BadRequestException | ConflictException ex) {
			model.addAttribute("error", ex.getMessage());
			return VIEW;
		}
	}

	record NameForm(String name) {
	}

}
