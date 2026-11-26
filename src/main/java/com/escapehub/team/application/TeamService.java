package com.escapehub.team.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.NotFoundException;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.domain.InviteCode;
import com.escapehub.team.domain.TeamName;
import com.escapehub.team.infrastructure.TeamEntity;
import com.escapehub.team.infrastructure.TeamMembershipEntity;
import com.escapehub.team.infrastructure.TeamMembershipRepository;
import com.escapehub.team.infrastructure.TeamRepository;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Ομάδες παικτών (US-03, US-04). Είναι το public API του module {@code team} για τα άλλα modules: π.χ. το
 * {@link #isCreator} και το {@link #isMember} για την έναρξη αποστολής (#27).
 */
@Service
public class TeamService {

	static final String TEAM_NOT_FOUND = "Η ομάδα δεν βρέθηκε.";

	static final String INVALID_CODE = "Ο κωδικός πρόσκλησης δεν αντιστοιχεί σε ομάδα.";

	private static final int MAX_CODE_ATTEMPTS = 10;

	private final TeamRepository teams;

	private final TeamMembershipRepository memberships;

	private final PlayerService players;

	public TeamService(TeamRepository teams, TeamMembershipRepository memberships, PlayerService players) {
		this.teams = teams;
		this.memberships = memberships;
		this.players = players;
	}

	/**
	 * Δημιουργεί ομάδα με τον παίκτη ως δημιουργό και νέο μοναδικό κωδικό πρόσκλησης.
	 *
	 * @throws BadRequestException αν το όνομα είναι κενό ή πολύ μεγάλο
	 */
	@Transactional
	public TeamDetails create(PlayerId creator, String rawName) {
		TeamName name = TeamName.of(rawName);
		TeamEntity team = teams.saveAndFlush(new TeamEntity(name.value(), newInviteCode(), creator.value()));
		memberships.saveIfAbsent(team.getId(), creator.value(), TeamRole.CREATOR.name());
		return details(new TeamId(team.getId()), creator);
	}

	/**
	 * Προσθέτει τον παίκτη στην ομάδα του κωδικού. Αν είναι ήδη μέλος, δεν προστίθεται ξανά.
	 *
	 * @throws BadRequestException αν ο κωδικός είναι κενός
	 * @throws NotFoundException αν ο κωδικός δεν αντιστοιχεί σε ομάδα
	 */
	@Transactional
	public JoinResult join(PlayerId player, String rawCode) {
		if (rawCode == null || rawCode.isBlank()) {
			throw new BadRequestException("Δώσε τον κωδικό πρόσκλησης.");
		}
		TeamEntity team = InviteCode.normalize(rawCode)
			.flatMap(teams::findByInviteCode)
			.orElseThrow(() -> new NotFoundException(INVALID_CODE));
		int added = memberships.saveIfAbsent(team.getId(), player.value(), TeamRole.MEMBER.name());
		return new JoinResult(details(new TeamId(team.getId()), player), added == 0);
	}

	/**
	 * Οι ομάδες του παίκτη, με τον ρόλο του σε καθεμία.
	 */
	@Transactional(readOnly = true)
	public List<TeamSummary> teamsOf(PlayerId player) {
		List<TeamMembershipEntity> mine = memberships.findByPlayerIdOrderByJoinedAtAscIdAsc(player.value());
		List<Long> teamIds = mine.stream().map(TeamMembershipEntity::getTeamId).toList();
		Map<Long, TeamEntity> byId = teams.findAllById(teamIds)
			.stream()
			.collect(Collectors.toMap(TeamEntity::getId, Function.identity()));
		return mine.stream()
			.filter(membership -> byId.containsKey(membership.getTeamId()))
			.map(membership -> new TeamSummary(new TeamId(membership.getTeamId()),
					byId.get(membership.getTeamId()).getName(), membership.getRole()))
			.toList();
	}

	/**
	 * Η ομάδα με τα μέλη και τον κωδικό της. Τη βλέπουν μόνο τα μέλη της.
	 *
	 * @throws NotFoundException αν η ομάδα δεν υπάρχει ή ο παίκτης δεν είναι μέλος της
	 */
	@Transactional(readOnly = true)
	public TeamDetails details(TeamId team, PlayerId viewer) {
		if (!isMember(team, viewer)) {
			throw new NotFoundException(TEAM_NOT_FOUND);
		}
		TeamEntity entity = teams.findById(team.value())
			.orElseThrow(() -> new NotFoundException(TEAM_NOT_FOUND));
		List<TeamMember> members = memberships.findByTeamIdOrderByJoinedAtAscIdAsc(team.value())
			.stream()
			.map(this::toMember)
			.toList();
		return new TeamDetails(team, entity.getName(), entity.getInviteCode(), members);
	}

	/**
	 * Είναι ο παίκτης μέλος της ομάδας (με οποιονδήποτε ρόλο);
	 */
	@Transactional(readOnly = true)
	public boolean isMember(TeamId team, PlayerId player) {
		return memberships.existsByTeamIdAndPlayerId(team.value(), player.value());
	}

	/**
	 * Είναι ο παίκτης ο δημιουργός της ομάδας;
	 */
	@Transactional(readOnly = true)
	public boolean isCreator(TeamId team, PlayerId player) {
		return memberships.existsByTeamIdAndPlayerIdAndRole(team.value(), player.value(), TeamRole.CREATOR);
	}

	private TeamMember toMember(TeamMembershipEntity membership) {
		PlayerId player = new PlayerId(membership.getPlayerId());
		String name = players.findName(player).orElse("Άγνωστος παίκτης");
		return new TeamMember(player, name, membership.getRole());
	}

	private String newInviteCode() {
		for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
			String code = InviteCode.generate();
			if (!teams.existsByInviteCode(code)) {
				return code;
			}
		}
		throw new IllegalStateException("Δεν βρέθηκε ελεύθερος κωδικός πρόσκλησης");
	}

}
