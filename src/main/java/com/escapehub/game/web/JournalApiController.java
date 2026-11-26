package com.escapehub.game.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.EvidenceService;
import com.escapehub.game.application.ExamineResult;
import com.escapehub.game.application.JournalItem;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * REST API για το κοινό ημερολόγιο ενός playthrough (US-07). Τα σφάλματα επιστρέφονται ως
 * {@code ProblemDetail}.
 */
@RestController
@RequestMapping("/api/playthroughs/{id}/journal")
class JournalApiController {

	private final EvidenceService evidence;

	JournalApiController(EvidenceService evidence) {
		this.evidence = evidence;
	}

	/**
	 * Το ημερολόγιο, με τα νεότερα πρώτα. Απαντά 403 αν ο παίκτης δεν είναι μέλος της ομάδας.
	 */
	@GetMapping
	List<JournalItemResponse> journal(@CurrentPlayer PlayerId player, @PathVariable long id) {
		return evidence.journal(player, playthrough(id)).stream().map(JournalItemResponse::of).toList();
	}

	/**
	 * Προσθέτει στοιχείο. Απαντά 201, ή 200 αν το στοιχείο ήταν ήδη στο ημερολόγιο (επιστρέφει την υπάρχουσα
	 * καταχώριση), 409 αν το στοιχείο δεν έχει αποκαλυφθεί και 403 αν ο παίκτης δεν είναι μέλος της ομάδας.
	 */
	@PostMapping
	ResponseEntity<JournalItemResponse> add(@CurrentPlayer PlayerId player, @PathVariable long id,
			@Valid @RequestBody AddRequest body) {
		ExamineResult result = evidence.examine(player, playthrough(id), body.evidenceId());
		JournalItemResponse response = JournalItemResponse.of(result.item());
		if (!result.added()) {
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.created(URI.create("/api/playthroughs/" + id + "/journal")).body(response);
	}

	private static PlaythroughId playthrough(long id) {
		if (id <= 0) {
			throw new NotFoundException("Το παιχνίδι δεν βρέθηκε.");
		}
		return new PlaythroughId(id);
	}

	record AddRequest(@NotBlank String evidenceId) {
	}

	record JournalItemResponse(String evidenceId, String title, long addedById, String addedBy, Instant addedAt) {

		static JournalItemResponse of(JournalItem item) {
			return new JournalItemResponse(item.evidenceId(), item.title(), item.addedBy().value(),
					item.addedByName(), item.addedAt());
		}

	}

}
