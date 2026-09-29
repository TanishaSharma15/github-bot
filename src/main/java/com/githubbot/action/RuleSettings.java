package com.githubbot.action;

record RuleSettings(String keyword, String label, boolean slackEnabled) {

	static RuleSettings defaults() {
		return new RuleSettings("bug", "bug", true);
	}

	static RuleSettings from(BotRule rule) {
		return new RuleSettings(rule.getKeyword(), rule.getLabel(), rule.isSlackEnabled());
	}

}
