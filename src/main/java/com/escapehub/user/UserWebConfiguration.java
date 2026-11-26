package com.escapehub.user;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ενεργοποιεί το {@link CurrentPlayer} σε όλους τους controllers.
 */
@Configuration(proxyBeanMethods = false)
class UserWebConfiguration implements WebMvcConfigurer {

	private final ObjectProvider<CurrentPlayerProvider> providers;

	UserWebConfiguration(ObjectProvider<CurrentPlayerProvider> providers) {
		this.providers = providers;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentPlayerArgumentResolver(providers));
	}

}
