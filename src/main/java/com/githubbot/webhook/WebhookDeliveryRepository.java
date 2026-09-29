package com.githubbot.webhook;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

	Optional<WebhookDelivery> findByDeliveryId(String deliveryId);

	long countByDeliveryId(String deliveryId);

}
