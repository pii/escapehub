package com.escapehub.user;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.escapehub.user.application.PlayerService;

/**
 * Για τις σελίδες του UI ({@code @Controller}):
 * <ul>
 * <li>χωρίς αναγνωρισμένο παίκτη, ανακατευθύνει στη σελίδα ονόματος,</li>
 * <li>προσθέτει στο model το όνομα του παίκτη ({@value #PLAYER_NAME}) για το header του layout.</li>
 * </ul>
 * Οι REST controllers δεν επηρεάζονται: εκεί το {@link CurrentPlayer} απαντά 401.
 */
class PlayerPageInterceptor implements HandlerInterceptor {

	static final String NAME_PAGE = "/player";

	static final String PLAYER_NAME = "currentPlayerName";

	private final ObjectProvider<CurrentPlayerProvider> providers;

	private final ObjectProvider<PlayerService> playerService;

	PlayerPageInterceptor(ObjectProvider<CurrentPlayerProvider> providers,
			ObjectProvider<PlayerService> playerService) {
		this.providers = providers;
		this.playerService = playerService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if (!isPage(handler) || currentPlayer(request).isPresent()) {
			return true;
		}
		response.sendRedirect(request.getContextPath() + NAME_PAGE);
		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		if (modelAndView == null || !isPage(handler) || isRedirect(modelAndView)) {
			return;
		}
		PlayerService service = playerService.getIfAvailable();
		if (service == null) {
			return;
		}
		currentPlayer(request)
			.flatMap(service::findName)
			.ifPresent(name -> modelAndView.addObject(PLAYER_NAME, name));
	}

	private Optional<PlayerId> currentPlayer(HttpServletRequest request) {
		CurrentPlayerProvider provider = providers.getIfAvailable();
		return (provider == null) ? Optional.empty() : provider.currentPlayer(request);
	}

	private static boolean isPage(Object handler) {
		return handler instanceof HandlerMethod method
				&& !AnnotatedElementUtils.hasAnnotation(method.getBeanType(), ResponseBody.class)
				&& !method.hasMethodAnnotation(ResponseBody.class);
	}

	private static boolean isRedirect(ModelAndView modelAndView) {
		String view = modelAndView.getViewName();
		return view != null && view.startsWith("redirect:");
	}

}
