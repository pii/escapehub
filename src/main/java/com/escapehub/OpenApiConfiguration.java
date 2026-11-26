package com.escapehub;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.escapehub.user.CurrentPlayer;

/**
 * Η τεκμηρίωση του REST API (TS-07), στο {@code /swagger-ui.html} και στο {@code /v3/api-docs}. Περιλαμβάνει μόνο
 * το {@code /api/**} ({@code springdoc.paths-to-match} στο {@code application.properties}).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	static {
		// Το @CurrentPlayer PlayerId το συμπληρώνει ο server από το cookie. Δεν είναι παράμετρος του αιτήματος.
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentPlayer.class);
	}

	@Bean
	OpenAPI escapehubOpenApi(ObjectProvider<BuildProperties> build) {
		BuildProperties properties = build.getIfAvailable();
		String version = (properties != null) ? properties.getVersion() : "dev";
		String description = "REST API του EscapeHub. Ο τρέχων παίκτης αναγνωρίζεται από το cookie που στέλνει"
				+ " το POST /api/players. Χωρίς αυτό, τα endpoints που χρειάζονται παίκτη απαντούν 401."
				+ " Τα σφάλματα επιστρέφονται ως ProblemDetail (RFC 9457).";
		return new OpenAPI().info(new Info().title("EscapeHub API").version(version).description(description));
	}

}
