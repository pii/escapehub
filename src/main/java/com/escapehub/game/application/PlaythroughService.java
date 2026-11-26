package com.escapehub.game.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.game.domain.Playthrough;
import com.escapehub.game.infrastructure.PlaythroughEntity;
import com.escapehub.game.infrastructure.PlaythroughRepository;
import com.escapehub.mission.application.MissionCatalog;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Κατάλογος αποστολών και έναρξη αποστολής (US-05, US-06). Είναι το public API του module {@code game} για τα
 * άλλα modules, π.χ. το {@link #details} για το ημερολόγιο (#28).
 */
@Service
public class PlaythroughService {

	static final String NOT_CREATOR = "Μόνο ο δημιουργός της ομάδας μπορεί να ξεκινήσει αποστολή.";

	static final String NOT_MEMBER = "Δεν είσαι μέλος αυτής της ομάδας.";

	static final String OTHER_TEAM = "Το παιχνίδι ανήκει σε ομάδα στην οποία δεν είσαι μέλος.";

	static final String PLAYTHROUGH_NOT_FOUND = "Το παιχνίδι δεν βρέθηκε.";

	/**
	 * Το όνομα της ομάδας ενός μέλους για το ατομικό παιχνίδι, π.χ. «Ατομικά: Μαρία».
	 */
	static final String SOLO_PREFIX = "Ατομικά: ";

	private final PlaythroughRepository playthroughs;

	private final MissionCatalog missions;

	private final TeamService teams;

	private final PlayerService players;

	public PlaythroughService(PlaythroughRepository playthroughs, MissionCatalog missions, TeamService teams,
			PlayerService players) {
		this.playthroughs = playthroughs;
		this.missions = missions;
		this.teams = teams;
		this.players = players;
	}

	/**
	 * Όλες οι αποστολές των αρχείων, με την κατάσταση για την ομάδα.
	 *
	 * @param team η ομάδα, ή {@code null} για κατάλογο χωρίς κατάσταση
	 * @throws ForbiddenException αν ο παίκτης δεν είναι μέλος της ομάδας
	 */
	@Transactional(readOnly = true)
	public List<CatalogEntry> catalog(PlayerId player, TeamId team) {
		if (team == null) {
			return missions.all().stream().map(mission -> CatalogEntry.of(mission, null, null)).toList();
		}
		if (!teams.isMember(team, player)) {
			throw new ForbiddenException(NOT_MEMBER);
		}
		Map<String, List<Playthrough>> byMission = playthroughs.findByTeamId(team.value())
			.stream()
			.map(PlaythroughService::toDomain)
			.collect(Collectors.groupingBy(Playthrough::missionId));
		return missions.all().stream().map(mission -> {
			List<Playthrough> ofMission = byMission.getOrDefault(mission.id(), List.of());
			PlaythroughId active = ofMission.stream()
				.filter(Playthrough::isActive)
				.map(Playthrough::id)
				.findFirst()
				.orElse(null);
			return CatalogEntry.of(mission, MissionStatus.of(ofMission), active);
		}).toList();
	}

	/**
	 * Ξεκινά την αποστολή για την ομάδα. Αν η ομάδα έχει ήδη ενεργό playthrough της αποστολής, επιστρέφει αυτό.
	 *
	 * @throws NotFoundException αν η αποστολή δεν υπάρχει
	 * @throws ForbiddenException αν ο παίκτης δεν είναι ο δημιουργός της ομάδας
	 */
	@Transactional
	public StartResult start(PlayerId player, TeamId team, String missionId) {
		Mission mission = missions.get(missionId);
		if (!teams.isCreator(team, player)) {
			throw new ForbiddenException(teams.isMember(team, player) ? NOT_CREATOR : NOT_MEMBER);
		}
		boolean created = playthroughs.startIfAbsent(team.value(), mission.id(), mission.version()) == 1;
		PlaythroughEntity active = playthroughs
			.findByTeamIdAndMissionIdAndCompletedAtIsNull(team.value(), mission.id())
			.orElseThrow(() -> new IllegalStateException("Λείπει το ενεργό playthrough μετά την έναρξη"));
		return new StartResult(toDetails(active, player), created);
	}

	/**
	 * Ατομικό παιχνίδι (US-06, AC4): ξεκινά την αποστολή με μια ομάδα που έχει μόνο τον παίκτη. Αν ο παίκτης έχει
	 * ήδη τέτοια ομάδα από προηγούμενο ατομικό παιχνίδι, τη χρησιμοποιεί ξανά.
	 *
	 * @throws NotFoundException αν η αποστολή δεν υπάρχει
	 */
	@Transactional
	public StartResult startSolo(PlayerId player, String missionId) {
		missions.get(missionId);
		return start(player, soloTeam(player), missionId);
	}

	/**
	 * Ένα playthrough. Το βλέπουν μόνο τα μέλη της ομάδας του.
	 *
	 * @throws NotFoundException αν δεν υπάρχει
	 * @throws ForbiddenException αν ο παίκτης δεν είναι μέλος της ομάδας
	 */
	@Transactional(readOnly = true)
	public PlaythroughDetails details(PlayerId player, PlaythroughId id) {
		PlaythroughEntity playthrough = playthroughs.findById(id.value())
			.orElseThrow(() -> new NotFoundException(PLAYTHROUGH_NOT_FOUND));
		if (!teams.isMember(new TeamId(playthrough.getTeamId()), player)) {
			throw new ForbiddenException(OTHER_TEAM);
		}
		return toDetails(playthrough, player);
	}

	private TeamId soloTeam(PlayerId player) {
		String name = SOLO_PREFIX + players.findName(player).orElse("παίκτης");
		for (TeamSummary team : teams.teamsOf(player)) {
			if (team.role() == TeamRole.CREATOR && team.name().equals(name)
					&& teams.details(team.id(), player).members().size() == 1) {
				return team.id();
			}
		}
		return teams.create(player, name).id();
	}

	private PlaythroughDetails toDetails(PlaythroughEntity entity, PlayerId viewer) {
		Playthrough playthrough = toDomain(entity);
		String teamName = teams.details(playthrough.team(), viewer).name();
		Optional<Mission> mission = missions.find(playthrough.missionId());
		String title = mission.map(Mission::title).orElse(playthrough.missionId());
		List<MissionPath> paths = mission.map(Mission::paths).orElse(List.of());
		return new PlaythroughDetails(playthrough.id(), playthrough.team(), teamName, playthrough.missionId(),
				playthrough.missionVersion(), title, paths, playthrough.startedAt(),
				playthrough.completedAt(), MissionStatus.of(playthrough));
	}

	private static Playthrough toDomain(PlaythroughEntity entity) {
		return new Playthrough(new PlaythroughId(entity.getId()), new TeamId(entity.getTeamId()),
				entity.getMissionId(), entity.getMissionVersion(), entity.getStartedAt(),
				entity.getCompletedAt());
	}

}
