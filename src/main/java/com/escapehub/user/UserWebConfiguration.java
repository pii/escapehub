package com.escapehub.user;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.escapehub.user.application.PlayerService;

/**
 * Ενεργοποιεί το {@link CurrentPlayer} σε όλους τους controllers, και την ανακατεύθυνση στη σελίδα ονόματος για
 * τις σελίδες του UI.
 */
@Configuration(proxyBeanMethods = false)
class UserWebConfiguration implements WebMvcConfigurer {

	private final ObjectProvider<CurrentPlayerProvider> providers;

	private final ObjectProvider<PlayerService> playerService;

	UserWebConfiguration(ObjectProvider<CurrentPlayerProvider> providers,
			ObjectProvider<PlayerService> playerService) {
		this.providers = providers;
		this.playerService = playerService;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentPlayerArgumentResolver(providers));
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new PlayerPageInterceptor(providers, playerService))
			.excludePathPatterns(PlayerPageInterceptor.NAME_PAGE, "/error", "/api/**", "/actuator/**");
	}

}
