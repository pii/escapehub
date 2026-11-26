package com.escapehub.user;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.escapehub.common.UnauthenticatedException;

/**
 * Γεμίζει τις παραμέτρους {@code @CurrentPlayer PlayerId} από τον {@link CurrentPlayerProvider}.
 */
class CurrentPlayerArgumentResolver implements HandlerMethodArgumentResolver {

	private final ObjectProvider<CurrentPlayerProvider> providers;

	CurrentPlayerArgumentResolver(ObjectProvider<CurrentPlayerProvider> providers) {
		this.providers = providers;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentPlayer.class)
				&& PlayerId.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		CurrentPlayerProvider provider = providers.getIfAvailable();
		Optional<PlayerId> player = (provider == null || request == null)
				? Optional.empty()
				: provider.currentPlayer(request);
		return player.orElseThrow(() -> new UnauthenticatedException("Δώστε πρώτα όνομα παίκτη."));
	}

}
