package com.githubbot.action;

import java.time.Instant;

import com.githubbot.webhook.WebhookDelivery;

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
@Table(name = "bot_actions", uniqueConstraints = @UniqueConstraint(columnNames = { "delivery_id", "action_type" }))
public class BotAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "delivery_id")
	private WebhookDelivery delivery;

	@Column(name = "action_type", nullable = false, length = 16)
	private String actionType;

	@Column(nullable = false, length = 16)
	private String status;

	@Column(nullable = false)
	private int attempts;

	@Column(nullable = false)
	private Instant nextAttemptAt;

	@Column(length = 500)
	private String lastError;

	public Long getId() {
		return id;
	}

	public WebhookDelivery getDelivery() {
		return delivery;
	}

	public void setDelivery(WebhookDelivery delivery) {
		this.delivery = delivery;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public void setNextAttemptAt(Instant nextAttemptAt) {
		this.nextAttemptAt = nextAttemptAt;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

}
