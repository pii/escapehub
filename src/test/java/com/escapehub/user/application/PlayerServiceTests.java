package com.escapehub.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.ConflictException;
import com.escapehub.user.PlayerId;
import com.escapehub.user.infrastructure.PlayerEntity;
import com.escapehub.user.infrastructure.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTests {

	@Mock
	private PlayerRepository repository;

	@Test
	void registersPlayerAndStoresOnlyTokenHash() {
		when(repository.existsByUsernameIgnoreCase("Μαρία")).thenReturn(false);
		when(repository.saveAndFlush(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 42L));

		RegisteredPlayer player = service().register("  Μαρία ");

		ArgumentCaptor<PlayerEntity> saved = ArgumentCaptor.forClass(PlayerEntity.class);
		verify(repository).saveAndFlush(saved.capture());
		assertThat(player.id()).isEqualTo(new PlayerId(42));
		assertThat(player.name()).isEqualTo("Μαρία");
		assertThat(player.token()).isNotBlank();
		assertThat(saved.getValue().getUsername()).isEqualTo("Μαρία");
		assertThat(saved.getValue().getTokenHash())
			.isEqualTo(PlayerTokens.hash(player.token()))
			.isNotEqualTo(player.token())
			.hasSize(64);
	}

	@Test
	void rejectsNameThatIsAlreadyTaken() {
		when(repository.existsByUsernameIgnoreCase("Maria")).thenReturn(true);

		assertThatThrownBy(() -> service().register("Maria"))
			.isInstanceOf(ConflictException.class)
			.hasMessage(PlayerService.NAME_TAKEN);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void concurrentRegistrationOfSameNameIsConflict() {
		when(repository.existsByUsernameIgnoreCase("Maria")).thenReturn(false);
		when(repository.saveAndFlush(any()))
			.thenThrow(new DataIntegrityViolationException("player_username_lower_uk"));

		assertThatThrownBy(() -> service().register("Maria"))
			.isInstanceOf(ConflictException.class)
			.hasMessage(PlayerService.NAME_TAKEN);
	}

	@Test
	void rejectsInvalidNameWithoutTouchingTheDatabase() {
		assertThatThrownBy(() -> service().register("ab")).isInstanceOf(BadRequestException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void findsPlayerByTokenHash() {
		when(repository.findByTokenHash(PlayerTokens.hash("token-1")))
			.thenReturn(Optional.of(withId(new PlayerEntity("Maria", "hash"), 5L)));

		assertThat(service().findByToken("token-1")).contains(new PlayerId(5));
	}

	@Test
	void unknownTokenFindsNoPlayer() {
		when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThat(service().findByToken("unknown")).isEmpty();
	}

	@Test
	void tokensAreRandomAndUrlSafe() {
		String first = PlayerTokens.newToken();
		String second = PlayerTokens.newToken();

		assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
	}

	private PlayerService service() {
		return new PlayerService(repository);
	}

	private static PlayerEntity withId(PlayerEntity entity, long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

}
