package com.githubbot.action;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.githubbot.webhook.WebhookDelivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BotActionRepository extends JpaRepository<BotAction, Long> {

	Optional<BotAction> findByDeliveryAndActionType(WebhookDelivery delivery, String actionType);

	@Query("""
			select a.id from BotAction a
			where a.status in ('pending', 'failed')
			and a.attempts < :maxAttempts
			and a.nextAttemptAt <= :now
			order by a.id
			""")
	List<Long> findDueIds(Instant now, int maxAttempts);

	@Query("select a from BotAction a where a.delivery.id in :deliveryIds order by a.id")
	List<BotAction> findByDeliveryIdIn(Collection<Long> deliveryIds);

}
