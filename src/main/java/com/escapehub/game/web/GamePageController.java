package com.escapehub.game.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.escapehub.common.ConflictException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.EvidenceService;
import com.escapehub.game.application.ExamineResult;
import com.escapehub.game.application.PlaythroughDetails;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.application.StartResult;
import com.escapehub.team.TeamId;
import com.escapehub.team.application.TeamService;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * Οι σελίδες «Αποστολές» (κατάλογος) και «Αποστολή σε εξέλιξη» με το κοινό ημερολόγιο (US-05, US-06, US-07).
 */
@Controller
class GamePageController {

	static final String STARTED = "Η αποστολή ξεκίνησε. Καλή επιτυχία!";

	static final String CONTINUED = "Η ομάδα έχει ήδη ενεργό παιχνίδι σε αυτή την αποστολή. Συνεχίζεις από εκεί.";

	/**
	 * Οι ώρες εμφανίζονται στην ώρα Ελλάδας, ανεξάρτητα από τη ζώνη ώρας του server.
	 */
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(ZoneId.of("Europe/Athens"));

	static final String ADDED = "Το στοιχείο προστέθηκε στο ημερολόγιο.";

	static final String ALREADY_IN_JOURNAL = "Το στοιχείο είναι ήδη στο ημερολόγιο.";

	private final PlaythroughService playthroughs;

	private final TeamService teams;

	private final EvidenceService evidence;

	GamePageController(PlaythroughService playthroughs, TeamService teams, EvidenceService evidence) {
		this.playthroughs = playthroughs;
		this.teams = teams;
		this.evidence = evidence;
	}

	/**
	 * Ο κατάλογος, με την κατάσταση κάθε αποστολής για την επιλεγμένη ομάδα. Χωρίς {@code team} επιλέγεται η
	 * πρώτη ομάδα του παίκτη.
	 */
	@GetMapping("/missions")
	String missions(@CurrentPlayer PlayerId player, @RequestParam(required = false) Long team, Model model) {
		List<TeamSummary> myTeams = teams.teamsOf(player);
		TeamSummary selected = null;
		if (team != null) {
			selected = myTeams.stream()
				.filter(candidate -> candidate.id().value() == team)
				.findFirst()
				.orElseThrow(() -> new ForbiddenException("Δεν είσαι μέλος αυτής της ομάδας."));
		}
		else if (!myTeams.isEmpty()) {
			selected = myTeams.get(0);
		}
		model.addAttribute("teams", myTeams);
		model.addAttribute("selectedTeam", selected);
		model.addAttribute("missions", playthroughs.catalog(player, (selected != null) ? selected.id() : null));
		return "game/missions";
	}

	/**
	 * Έναρξη για την ομάδα. Αν ο παίκτης δεν είναι ο δημιουργός, επιστρέφει στον κατάλογο με μήνυμα (US-06, AC2).
	 */
	@PostMapping("/playthroughs")
	String start(@CurrentPlayer PlayerId player, @RequestParam long teamId, @RequestParam String missionId,
			RedirectAttributes redirect) {
		try {
			if (teamId <= 0) {
				throw new ForbiddenException("Δεν είσαι μέλος αυτής της ομάδας.");
			}
			return toPlaythrough(playthroughs.start(player, new TeamId(teamId), missionId), redirect);
		}
		catch (ForbiddenException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
			return "redirect:/missions?team=" + teamId;
		}
	}

	/**
	 * Ατομικό παιχνίδι, ως ομάδα ενός μέλους (US-06, AC4).
	 */
	@PostMapping("/playthroughs/solo")
	String startSolo(@CurrentPlayer PlayerId player, @RequestParam String missionId, RedirectAttributes redirect) {
		return toPlaythrough(playthroughs.startSolo(player, missionId), redirect);
	}

	/**
	 * Η αποστολή σε εξέλιξη: τίτλος, ομάδα, χρόνος έναρξης, διαδρομές, αντικείμενα για εξέταση και το κοινό
	 * ημερολόγιο (US-07, AC5). Τη βλέπουν μόνο τα μέλη της ομάδας. Οι καταχωρίσεις των συμπαικτών φαίνονται με
	 * ανανέωση της σελίδας.
	 */
	@GetMapping("/playthroughs/{id}")
	String playthrough(@CurrentPlayer PlayerId player, @PathVariable long id, Model model) {
		PlaythroughId playthroughId = playthroughId(id);
		PlaythroughDetails playthrough = playthroughs.details(player, playthroughId);
		model.addAttribute("playthrough", playthrough);
		model.addAttribute("startedAt", TIME.format(playthrough.startedAt()));
		model.addAttribute("examinable", evidence.examinable(player, playthroughId));
		model.addAttribute("journal", evidence.journal(player, playthroughId));
		model.addAttribute("time", TIME);
		return "game/playthrough";
	}

	/**
	 * Εξέταση αντικειμένου: το στοιχείο μπαίνει στο ημερολόγιο. Αν δεν είναι διαθέσιμο, η σελίδα δείχνει μήνυμα.
	 */
	@PostMapping("/playthroughs/{id}/journal")
	String examine(@CurrentPlayer PlayerId player, @PathVariable long id, @RequestParam String evidenceId,
			RedirectAttributes redirect) {
		try {
			ExamineResult result = evidence.examine(player, playthroughId(id), evidenceId);
			redirect.addFlashAttribute("message", result.added() ? ADDED : ALREADY_IN_JOURNAL);
		}
		catch (ConflictException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/playthroughs/" + id;
	}

	private static PlaythroughId playthroughId(long id) {
		if (id <= 0) {
			throw new NotFoundException("Το παιχνίδι δεν βρέθηκε.");
		}
		return new PlaythroughId(id);
	}

	private static String toPlaythrough(StartResult result, RedirectAttributes redirect) {
		redirect.addFlashAttribute("message", result.created() ? STARTED : CONTINUED);
		return "redirect:/playthroughs/" + result.playthrough().id().value();
	}

}
