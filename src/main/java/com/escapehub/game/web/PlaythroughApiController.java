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

import com.escapehub.common.BadRequestException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.PlaythroughDetails;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.application.StartResult;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.team.TeamId;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * REST API για την έναρξη αποστολής και την προβολή ενός playthrough (US-06). Τα σφάλματα επιστρέφονται ως
 * {@code ProblemDetail}.
 */
@RestController
@RequestMapping("/api/playthroughs")
class PlaythroughApiController {

	private final PlaythroughService playthroughs;

	PlaythroughApiController(PlaythroughService playthroughs) {
		this.playthroughs = playthroughs;
	}

	/**
	 * Ξεκινά αποστολή για την ομάδα {@code teamId}, ή ατομικά με {@code solo: true}. Απαντά 201 για νέο
	 * playthrough, 200 αν η ομάδα είχε ήδη ενεργό playthrough της αποστολής (το επιστρέφει), 403 αν ο παίκτης δεν
	 * είναι ο δημιουργός της ομάδας και 404 για άγνωστη αποστολή.
	 */
	@PostMapping
	ResponseEntity<PlaythroughResponse> start(@CurrentPlayer PlayerId player,
			@Valid @RequestBody StartRequest body) {
		StartResult result;
		if (body.soloPlay()) {
			if (body.teamId() != null) {
				throw new BadRequestException("Το ατομικό παιχνίδι δεν παίρνει teamId.");
			}
			result = playthroughs.startSolo(player, body.missionId());
		}
		else {
			if (body.teamId() == null) {
				throw new BadRequestException("Δώσε teamId, ή solo: true για ατομικό παιχνίδι.");
			}
			if (body.teamId() <= 0) {
				throw new ForbiddenException("Δεν είσαι μέλος αυτής της ομάδας.");
			}
			result = playthroughs.start(player, new TeamId(body.teamId()), body.missionId());
		}
		PlaythroughResponse response = PlaythroughResponse.of(result.playthrough());
		if (!result.created()) {
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.created(URI.create("/api/playthroughs/" + response.id())).body(response);
	}

	/**
	 * Ένα playthrough. Απαντά 403 αν ο παίκτης δεν είναι μέλος της ομάδας του, ή 404 αν δεν υπάρχει.
	 */
	@GetMapping("/{id}")
	PlaythroughResponse playthrough(@CurrentPlayer PlayerId player, @PathVariable long id) {
		if (id <= 0) {
			throw new NotFoundException("Το παιχνίδι δεν βρέθηκε.");
		}
		return PlaythroughResponse.of(playthroughs.details(player, new PlaythroughId(id)));
	}

	/**
	 * Το {@code solo} είναι {@code Boolean} και όχι {@code boolean}: το Jackson 3 απορρίπτει με 400 ένα αίτημα
	 * όπου λείπει πεδίο primitive τύπου σε record.
	 *
	 * @param missionId η αποστολή
	 * @param teamId η ομάδα, όταν δεν είναι ατομικό παιχνίδι
	 * @param solo {@code true} για ατομικό παιχνίδι, ως ομάδα ενός μέλους. Αν λείπει, {@code false}.
	 */
	record StartRequest(@NotBlank String missionId, Long teamId, Boolean solo) {

		boolean soloPlay() {
			return Boolean.TRUE.equals(solo);
		}

	}

	record PlaythroughResponse(long id, long teamId, String teamName, String missionId, int missionVersion,
			String missionTitle, MissionStatus status, Instant startedAt, Instant completedAt,
			List<PathResponse> paths) {

		static PlaythroughResponse of(PlaythroughDetails details) {
			List<PathResponse> paths = details.paths()
				.stream()
				.map(path -> new PathResponse(path.id(), path.title()))
				.toList();
			return new PlaythroughResponse(details.id().value(), details.team().value(), details.teamName(),
					details.missionId(), details.missionVersion(), details.missionTitle(),
					details.status(), details.startedAt(), details.completedAt(), paths);
		}

	}

	record PathResponse(String id, String title) {
	}

}
