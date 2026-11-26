package com.escapehub.game.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.escapehub.common.ForbiddenException;
import com.escapehub.game.application.CatalogEntry;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.team.TeamId;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * REST API για τον κατάλογο αποστολών (US-05). Τα σφάλματα επιστρέφονται ως {@code ProblemDetail}.
 */
@RestController
@RequestMapping("/api/missions")
class MissionCatalogApiController {

	private final PlaythroughService playthroughs;

	MissionCatalogApiController(PlaythroughService playthroughs) {
		this.playthroughs = playthroughs;
	}

	/**
	 * Οι αποστολές. Με {@code teamId}, κάθε αποστολή έχει και την κατάσταση για την ομάδα. Απαντά 403 αν ο
	 * παίκτης δεν είναι μέλος της ομάδας.
	 */
	@GetMapping
	List<MissionResponse> catalog(@CurrentPlayer PlayerId player, @RequestParam(required = false) Long teamId) {
		TeamId team = null;
		if (teamId != null) {
			if (teamId <= 0) {
				throw new ForbiddenException("Δεν είσαι μέλος αυτής της ομάδας.");
			}
			team = new TeamId(teamId);
		}
		return playthroughs.catalog(player, team).stream().map(MissionResponse::of).toList();
	}

	/**
	 * Μια αποστολή του καταλόγου. Τα {@code status} και {@code playthroughId} υπάρχουν μόνο με {@code teamId}.
	 */
	record MissionResponse(String id, String title, String summary, int estimatedMinutes, int minPlayers,
			MissionStatus status, Long playthroughId) {

		static MissionResponse of(CatalogEntry entry) {
			Long playthroughId = null;
			if (entry.activePlaythrough() != null) {
				playthroughId = entry.activePlaythrough().value();
			}
			return new MissionResponse(entry.missionId(), entry.title(), entry.summary(),
					entry.estimatedMinutes(), entry.minPlayers(), entry.status(), playthroughId);
		}

	}

}
