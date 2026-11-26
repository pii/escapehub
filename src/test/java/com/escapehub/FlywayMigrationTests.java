package com.escapehub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapehub.support.IntegrationTest;

@IntegrationTest
class FlywayMigrationTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void allMigrationsAppliedSuccessfully() {
		Integer failed = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where not success", Integer.class);
		Integer baseline = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '202611261200' and success",
				Integer.class);

		assertThat(failed).isZero();
		assertThat(baseline).isEqualTo(1);
	}

}
