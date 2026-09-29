package com.githubbot.webhook;

import java.util.List;
import java.util.Optional;

import com.githubbot.auth.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

	Optional<WebhookDelivery> findByDeliveryId(String deliveryId);

	long countByDeliveryId(String deliveryId);

	@Query("select d.id from WebhookDelivery d where d.status = :status order by d.id")
	List<Long> findIdsByStatus(String status);

	@Query("""
			select d from WebhookDelivery d
			join fetch d.repository r
			where r.user = :user
			order by d.receivedAt desc, d.id desc
			""")
	List<WebhookDelivery> findForUser(User user);

}
