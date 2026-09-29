package com.githubbot.repo;

import java.util.List;
import java.util.Optional;

import com.githubbot.auth.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedRepositoryRepository extends JpaRepository<TrackedRepository, Long> {

	Optional<TrackedRepository> findByUserAndGithubRepoId(User user, Long githubRepoId);

	Optional<TrackedRepository> findFirstByWebhookId(Long webhookId);

	List<TrackedRepository> findByUserOrderByNameAsc(User user);

}
