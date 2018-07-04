package application.event;

import core.Event;

public class BroadcastEvent extends Event {
	
	private Object object;
	private String topic;
	
	public BroadcastEvent(Object object, String topic) {

		this.object = object;
		this.topic = topic;
		
	}
	
	@Override
	public Object getObject() {
		return this.object;
	}
	
	public String getTopic() {
		return this.topic;
	}

}
