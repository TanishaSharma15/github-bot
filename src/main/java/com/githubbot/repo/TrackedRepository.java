package com.githubbot.repo;

import com.githubbot.auth.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "repositories", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "github_repo_id" }))
public class TrackedRepository {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false)
	private Long githubRepoId;

	@Column(nullable = false)
	private String owner;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Long webhookId;

	@Column(nullable = false, length = 2048)
	private String webhookSecret;

	@Column(nullable = false, length = 2048)
	private String slackWebhookUrl;

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getGithubRepoId() {
		return githubRepoId;
	}

	public void setGithubRepoId(Long githubRepoId) {
		this.githubRepoId = githubRepoId;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getWebhookId() {
		return webhookId;
	}

	public void setWebhookId(Long webhookId) {
		this.webhookId = webhookId;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getSlackWebhookUrl() {
		return slackWebhookUrl;
	}

	public void setSlackWebhookUrl(String slackWebhookUrl) {
		this.slackWebhookUrl = slackWebhookUrl;
	}

}
