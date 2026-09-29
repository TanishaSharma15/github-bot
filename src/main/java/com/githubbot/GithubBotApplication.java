package com.githubbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GithubBotApplication {

	public static void main(String[] args) {
		DotEnv.load();
		SpringApplication.run(GithubBotApplication.class, args);
	}

}
