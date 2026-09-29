package com.githubbot.summary;

public interface SummaryClient {

	boolean isConfigured();

	String summarize(String title, String body);

}
