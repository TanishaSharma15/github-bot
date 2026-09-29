package com.githubbot.action;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BotActionWorker {

	private final BotActionService actions;

	public BotActionWorker(BotActionService actions) {
		this.actions = actions;
	}

	@Scheduled(initialDelayString = "${app.worker-delay-ms:15000}", fixedDelayString = "${app.worker-delay-ms:15000}")
	public void process() {
		for (Long deliveryId : actions.receivedDeliveryIds()) {
			actions.plan(deliveryId);
		}
		for (Long actionId : actions.dueActionIds()) {
			actions.run(actionId);
		}
	}

}
