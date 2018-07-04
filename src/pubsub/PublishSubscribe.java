package pubsub;

import peersim.core.Node;
import peersim.core.Protocol;

public interface PublishSubscribe extends Protocol {
	
	public void publish(Node node, String topic, Object m);
	public void subscribe(Node node, String topic);
	public void unsubscribe(Node node, String topic);

}
