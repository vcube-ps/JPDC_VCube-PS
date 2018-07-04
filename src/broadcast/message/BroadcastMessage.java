package broadcast.message;

import peersim.core.Node;

public class BroadcastMessage extends AbstractBroadcastMessage {
	
	private int topicRoot;

	public BroadcastMessage(MessageType type, Node source, String topic, int counter, Object data) {
		
		super(type.name(), source, topic, counter, data);
		
	}

	public int getTopicRoot() {
		return topicRoot;
	}

	public void setTopicRoot(int rootTopic) {
		this.topicRoot = rootTopic;
	}	
	
}
