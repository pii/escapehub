package com.escapehub.team.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.escapehub.team.TeamRole;

/**
 * Πρόσβαση στον πίνακα {@code team_membership}.
 */
public interface TeamMembershipRepository extends JpaRepository<TeamMembershipEntity, Long> {

	/**
	 * Προσθέτει τον παίκτη στην ομάδα, αν δεν είναι ήδη μέλος. Η απόφαση γίνεται στη βάση
	 * ({@code on conflict do nothing}), οπότε ισχύει και για ταυτόχρονα αιτήματα.
	 *
	 * @return 1 αν προστέθηκε, 0 αν ήταν ήδη μέλος
	 */
	@Modifying
	@Query(value = """
			insert into team_membership (team_id, player_id, role, joined_at)
			values (:teamId, :playerId, :role, now())
			on conflict (team_id, player_id) do nothing
			""", nativeQuery = true)
	int saveIfAbsent(@Param("teamId") long teamId, @Param("playerId") long playerId, @Param("role") String role);

	boolean existsByTeamIdAndPlayerId(Long teamId, Long playerId);

	boolean existsByTeamIdAndPlayerIdAndRole(Long teamId, Long playerId, TeamRole role);

	List<TeamMembershipEntity> findByTeamIdOrderByJoinedAtAscIdAsc(Long teamId);

	List<TeamMembershipEntity> findByPlayerIdOrderByJoinedAtAscIdAsc(Long playerId);

}
