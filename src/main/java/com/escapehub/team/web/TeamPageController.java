package com.escapehub.team.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.NotFoundException;
import com.escapehub.team.TeamId;
import com.escapehub.team.application.JoinResult;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * Οι σελίδες «Οι ομάδες μου» και «Ομάδα» (US-03, US-04).
 */
@Controller
@RequestMapping("/teams")
class TeamPageController {

	private static final String TEAMS_VIEW = "team/teams";

	private static final String TEAM_VIEW = "team/team";

	private final TeamService teams;

	TeamPageController(TeamService teams) {
		this.teams = teams;
	}

	@GetMapping
	String teams(@CurrentPlayer PlayerId player, Model model) {
		model.addAttribute("teams", teams.teamsOf(player));
		return TEAMS_VIEW;
	}

	@PostMapping
	String create(@CurrentPlayer PlayerId player, @RequestParam(defaultValue = "") String name, Model model,
			RedirectAttributes redirect) {
		try {
			TeamDetails team = teams.create(player, name);
			redirect.addFlashAttribute("message", "Η ομάδα «" + team.name()
					+ "» δημιουργήθηκε. Δώσε τον κωδικό πρόσκλησης στους συμπαίκτες σου.");
			return "redirect:/teams/" + team.id().value();
		}
		catch (BadRequestException ex) {
			return teamsWithError(player, model, ex.getMessage());
		}
	}

	@PostMapping("/join")
	String join(@CurrentPlayer PlayerId player, @RequestParam(defaultValue = "") String inviteCode, Model model,
			RedirectAttributes redirect) {
		try {
			JoinResult result = teams.join(player, inviteCode);
			String name = result.team().name();
			redirect.addFlashAttribute("message", result.alreadyMember()
					? "Είσαι ήδη μέλος της ομάδας «" + name + "»."
					: "Μπήκες στην ομάδα «" + name + "».");
			return "redirect:/teams/" + result.team().id().value();
		}
		catch (BadRequestException | NotFoundException ex) {
			return teamsWithError(player, model, ex.getMessage());
		}
	}

	@GetMapping("/{id}")
	String team(@CurrentPlayer PlayerId player, @PathVariable long id, Model model) {
		if (id <= 0) {
			throw new NotFoundException("Η ομάδα δεν βρέθηκε.");
		}
		model.addAttribute("team", teams.details(new TeamId(id), player));
		return TEAM_VIEW;
	}

	private String teamsWithError(PlayerId player, Model model, String error) {
		model.addAttribute("error", error);
		model.addAttribute("teams", teams.teamsOf(player));
		return TEAMS_VIEW;
	}

}
