package application.event;

import core.Event;

public class UnsubscribeEvent extends Event {
	
	private String topic;

	public UnsubscribeEvent(String topic) {

		this.topic = topic;

	}

	public String getTopic() {
		return this.topic;
	}

}
