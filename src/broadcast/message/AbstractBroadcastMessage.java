package broadcast.message;

import core.Message;
import peersim.core.Node;

public abstract class AbstractBroadcastMessage extends Message {
	
	protected String topic;
	protected Integer counter;
	protected Object data;
	protected long waitTime = 0;
	
	public AbstractBroadcastMessage(String type, Node source, String topic, Integer counter, Object data) {
		
		super(type, source, null);
		
		this.topic = topic;
		this.counter = counter;
		this.data = data;
		
	}
	
	public String getTopic() {
		return this.topic;
	}	
	
	public Integer getCounter() {
		return this.counter;
	}
	
	public Object getData() {
		return this.data;
	}
	
	public long getWaitTime() {
		return this.waitTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	@Override
	public String toString() {
		return String.format("<%s, %d, %s, %d>", this.getType(), this.getSource().getID(), this.getTopic(), this.getCounter());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((counter == null) ? 0 : counter.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		result = prime * result + (int) (waitTime ^ (waitTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof AbstractBroadcastMessage)) {
			return false;
		}
		AbstractBroadcastMessage other = (AbstractBroadcastMessage) obj;
		if (counter == null) {
			if (other.counter != null) {
				return false;
			}
		} else if (!counter.equals(other.counter)) {
			return false;
		}
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else if (!data.equals(other.data)) {
			return false;
		}
		if (topic == null) {
			if (other.topic != null) {
				return false;
			}
		} else if (!topic.equals(other.topic)) {
			return false;
		}
		if (waitTime != other.waitTime) {
			return false;
		}
		return true;
	}
	
}
