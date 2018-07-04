package core;

import peersim.core.CommonState;

public class Event {
	
	private long creationTime;
	private Object object;

	public Event() {
		
		this.creationTime = CommonState.getTime();
		
	}
	
	public Event(Object object) {
		
		this();
		
		this.object = object;
		
	}
	
	public long getCreationTime() {
		return this.creationTime;
	}

	public Object getObject() {
		return this.object;
	}	

}
