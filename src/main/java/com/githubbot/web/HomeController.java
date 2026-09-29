package com.githubbot.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Authentication authentication, Model model) {
		model.addAttribute("login", loginOf(authentication));
		return "home";
	}

	static String loginOf(Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		if (authentication.getPrincipal() instanceof OAuth2User user) {
			Object login = user.getAttribute("login");
			if (login != null) {
				return login.toString();
			}
		}
		return authentication.getName();
	}

}
