package com.githubbot.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

	@GetMapping("/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("login", HomeController.loginOf(authentication));
		return "dashboard";
	}

}
