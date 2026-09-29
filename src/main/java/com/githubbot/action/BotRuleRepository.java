package com.githubbot.action;

import java.util.Optional;

import com.githubbot.auth.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRuleRepository extends JpaRepository<BotRule, Long> {

	Optional<BotRule> findByUser(User user);

}
