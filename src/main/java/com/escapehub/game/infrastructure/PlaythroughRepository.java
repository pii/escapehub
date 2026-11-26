package com.escapehub.game.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Πρόσβαση στον πίνακα {@code playthrough}.
 */
public interface PlaythroughRepository extends JpaRepository<PlaythroughEntity, Long> {

	/**
	 * Ξεκινά playthrough της αποστολής για την ομάδα, αν δεν έχει ήδη ενεργό. Η απόφαση γίνεται στη βάση
	 * ({@code on conflict do nothing} στο partial unique index), οπότε ισχύει και για ταυτόχρονα αιτήματα.
	 *
	 * @return 1 αν ξεκίνησε νέο, 0 αν υπήρχε ήδη ενεργό
	 */
	@Modifying
	@Query(value = """
			insert into playthrough (team_id, mission_id, mission_version, started_at)
			values (:teamId, :missionId, :missionVersion, now())
			on conflict (team_id, mission_id) where completed_at is null do nothing
			""", nativeQuery = true)
	int startIfAbsent(@Param("teamId") long teamId, @Param("missionId") String missionId,
			@Param("missionVersion") int missionVersion);

	Optional<PlaythroughEntity> findByTeamIdAndMissionIdAndCompletedAtIsNull(Long teamId, String missionId);

	List<PlaythroughEntity> findByTeamId(Long teamId);

}
