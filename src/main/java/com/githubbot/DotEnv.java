package com.githubbot;

import java.nio.file.Files;
import java.nio.file.Path;

final class DotEnv {

	private DotEnv() {
	}

	static void load() {
		Path path = Path.of(".env");
		if (!Files.isRegularFile(path)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(path)) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
					continue;
				}
				int separator = trimmed.indexOf('=');
				String key = trimmed.substring(0, separator).trim();
				String value = stripQuotes(trimmed.substring(separator + 1).trim());
				if (key.isEmpty() || System.getenv(key) != null || System.getProperty(key) != null) {
					continue;
				}
				System.setProperty(key, value);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not read .env", ex);
		}
	}

	private static String stripQuotes(String value) {
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

}
