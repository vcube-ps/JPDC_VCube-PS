package application;

import application.event.BroadcastEvent;
import application.event.SubscribeEvent;
import application.event.UnsubscribeEvent;
import broadcast.message.BroadcastMessage;
import peersim.core.Node;
import pubsub.PublishSubscribe;

public class InputApplication extends AbsctractApplication {

	public InputApplication(String prefix) {
		
		super(prefix);
		
		oracle.setApplicationType("INPUT");
		
	}

	@Override
	public void processEvent(Node node, int protocolID, Object event) {
		
		if (event instanceof BroadcastMessage) {
			
			this.receiveMessage(node, event, protocolID);
			
		} else if (event instanceof BroadcastEvent) {
			
			BroadcastEvent broadcastEvent = (BroadcastEvent) event;
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.publish(
				node, 
				broadcastEvent.getTopic(),
				broadcastEvent.getObject()
			);
			
		} else if (event instanceof SubscribeEvent) {
			
			SubscribeEvent subscribeEvent = ((SubscribeEvent) event);
			
			String topic = subscribeEvent.getTopic();
			
			logger.out(node.getID(), "SUBSCRIBE; TOPIC \"%s\";\n", topic);
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.subscribe(node, topic);			
						
		} else if (event instanceof UnsubscribeEvent) {
			
			String topic = ((UnsubscribeEvent) event).getTopic();
			
			logger.out(node.getID(), "UNSUBSCRIBE; TOPIC \"%s\";\n", 
				((UnsubscribeEvent) event).getTopic()
			);
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.unsubscribe(node, topic);			

		}
		
	}

}
