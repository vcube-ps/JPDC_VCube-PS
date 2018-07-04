package application.event;

import core.Event;

public class SubscribeEvent extends Event {
	
	private String topic;

	public SubscribeEvent(String topic) {

		this.topic = topic;
	}
	
	public String getTopic() {
		return this.topic;
	}
	
}
