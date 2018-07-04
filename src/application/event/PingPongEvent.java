package application.event;

import core.Event;

public class PingPongEvent extends Event {
	
	private String topic;
	
	public PingPongEvent(String topic) {
		this.topic = topic;
		
	}
	
	public String getTopic() {
		return this.topic;
	}

}
