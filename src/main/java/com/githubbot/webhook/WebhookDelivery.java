package com.githubbot.webhook;

import java.time.Instant;

import com.githubbot.repo.TrackedRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String deliveryId;

	@Column(nullable = false, length = 64)
	private String event;

	@ManyToOne(optional = false)
	@JoinColumn(name = "repository_id")
	private TrackedRepository repository;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String payload;

	@Column(nullable = false)
	private Instant receivedAt;

	@Column(nullable = false, length = 32)
	private String status;

	public Long getId() {
		return id;
	}

	public String getDeliveryId() {
		return deliveryId;
	}

	public void setDeliveryId(String deliveryId) {
		this.deliveryId = deliveryId;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public TrackedRepository getRepository() {
		return repository;
	}

	public void setRepository(TrackedRepository repository) {
		this.repository = repository;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(Instant receivedAt) {
		this.receivedAt = receivedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
