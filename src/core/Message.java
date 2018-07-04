package core;

import peersim.core.CommonState;
import peersim.core.Node;

public class Message {
	
	protected Node source;
	protected Node destination;
	protected String type;
	protected long creationTime;

	public Message(Node source, Node destination) {
		
		this.source = source;
		this.destination = destination;
		this.creationTime = CommonState.getTime();
		
	}
	
	public Message(String type, Node source, Node destination) {
		
		this.type = type;
		this.source = source;
		this.destination = destination;
		this.creationTime = CommonState.getTime();
		
	}

	public Node getSource() {
		return this.source;
	}
	
	public Node getDestination() {
		return this.destination;
	}
	
	public long getCreationTime() {
		return this.creationTime;
	}
	
	public String getType() {
		return this.type;
	}

	public void setDestination(Node destination) {
		this.destination = destination;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		
		return String.format(
			"[%s] Packet from %d to %d created at %d", 
			this.getClass().getName(), this.source.getID(), 
			this.destination.getID(), this.creationTime 
		);
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.creationTime ^ (this.creationTime >>> 32));
		result = prime * result + ((this.destination == null) ? 0 : this.destination.hashCode());
		result = prime * result + ((this.source == null) ? 0 : this.source.hashCode());
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (this.creationTime != other.creationTime)
			return false;
		if (this.destination == null) {
			if (other.destination != null)
				return false;
		} else if (!this.destination.equals(other.destination))
			return false;
		if (this.source == null) {
			if (other.source != null)
				return false;
		} else if (!this.source.equals(other.source))
			return false;
		if (this.type == null) {
			if (other.type != null)
				return false;
		} else if (!this.type.equals(other.type))
			return false;
		return true;
	}

}
