package pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import broadcast.Broadcast;
import broadcast.ViewItem;
import broadcast.ViewItem.Action;
import broadcast.message.BroadcastMessage;
import broadcast.message.MessageType;
import core.Oracle;
import logger.Logger;
import peersim.config.Configuration;
import peersim.core.Node;

public class TopicPublishSubscribe implements PublishSubscribe {
	
	private static final String PAR_LOWER = "lower";
	private static final String PAR_LOGGER_LEVEL = "logger_level";
	
	private static int loggerLevel;
	private static int lowerID;
	private static Logger logger;
	private static Oracle oracle = Oracle.getInstance();

	private static final int topicRoot = 0;
	
	private Map<String, Integer> counter = new HashMap<String, Integer>();

	public TopicPublishSubscribe(String prefix) {

		lowerID = Configuration.getPid(prefix + "." + PAR_LOWER);
		loggerLevel = Configuration.getInt(prefix + "." + PAR_LOGGER_LEVEL, Logger.INFO);
		
		logger = new Logger(this.getClass().getName(), loggerLevel);
		
	}
	
	@Override
	public void publish(Node node, String topic, Object message) {
		
		Broadcast broadcast = (Broadcast) node.getProtocol(lowerID);
		
		if (broadcast.matchView(broadcast.getView(topic), new ViewItem((int)node.getID(), ViewItem.Action.JOIN, null)) == null ) {
			
			logger.err(Logger.ALWAYS, node.getID(), 
				"PUBLISH; NOT_MEMBER %s; MESSAGE %s\n", 
				topic, message
			);
			
			return;
			
		}

		BroadcastMessage broadcastMessage = new BroadcastMessage(
			MessageType.PUB,
			node, 
			topic,
			this.nextCounter(topic),
			message
		);
		
		broadcastMessage.setTopicRoot(topicRoot);
		
		logger.out(node.getID(), "PUBLISH; BROADCAST; MESSAGE %s; DATA \"%s\";\n", 
				broadcastMessage, broadcastMessage.getData()
		);
					
		broadcast.broadcast(node, broadcastMessage, lowerID);

		oracle.getStats("SENT_APPLICATION").add(1);		
		
	}

	@Override
	public void subscribe(Node node, String topic) {
		
		Broadcast broadcast = (Broadcast) node.getProtocol(lowerID);
		
		if (broadcast.matchView(broadcast.getView(topic), new ViewItem((int)node.getID(), ViewItem.Action.JOIN, null)) != null ) {
			
			logger.err(Logger.INFO, node.getID(), 
				"SUBSCRIBE; ALREADY_SUBSCRIBED; TOPIC \"%s\";\n", 
				topic
			);
				
			return;
			
		}
		
		int i = (int) node.getID();
		int c = this.nextCounter(topic);
		
		// In order to make faster the first subscriptions in
		// ONE SINGLE TOPIC and STATIC SUBSCRIBERS
		// simulations, we make it simpler using our
		// most beloved Oracle o/
		
		if (! oracle.isFastSubscription()) {
			
			broadcast.getView(topic).clear();
			
			BroadcastMessage broadcastMessage = new BroadcastMessage(
				MessageType.SUB,
				node, 
				topic,
				c,
				null
			);
			
			broadcast.broadcast(node, broadcastMessage, lowerID);
		
		}
		
		broadcast.getView(topic).add(new ViewItem(i, ViewItem.Action.JOIN, c));
			
		logger.out(Logger.INFO, node.getID(), "SUBSCRIBE; TOPIC \"%s\";\n", topic);

		oracle.getStats("SUBSCRIPTIONS").add(1);
		
		if (broadcast.allowForwarders() && broadcast.isSingleRoot()) {

			Integer firstNeighbor = topicRoot;
			Integer cluster = broadcast.cluster(topicRoot, i);
			
			do {
				
				if (broadcast.matchView(broadcast.getView(topic), new ViewItem(firstNeighbor, ViewItem.Action.JOIN, null)) == null) {
					
					c = this.nextCounter(topic);
					
					if (oracle.isFastSubscription()) {
						
						ViewItem oldItem = broadcast.matchView(broadcast.getView(topic), new ViewItem(firstNeighbor, null, null));
						
						if (oldItem != null) {
							broadcast.getView(topic).remove(oldItem);
						}
												
						broadcast.getView(topic).add(new ViewItem((int) firstNeighbor, ViewItem.Action.FORWARD, c));
								
					} 
					
				}

				firstNeighbor = broadcast.firstFaultFreeNeighbor(firstNeighbor, cluster, null);
				cluster = broadcast.cluster(firstNeighbor, (int) node.getID());
				
			} while (firstNeighbor != node.getID()); 

		}
		
	}

	@Override
	public void unsubscribe(Node node, String topic) {
		
		Broadcast broadcast = (Broadcast) node.getProtocol(lowerID);
		
		ViewItem match = broadcast.matchView(broadcast.getView(topic), new ViewItem((int)node.getID(), ViewItem.Action.JOIN, null));
		
		if (match == null ) {
			
			logger.err(Logger.INFO, node.getID(), 
				"UNSUBSCRIBE; NOT_SUBSCRIBED; TOPIC \"%s\";\n", 
				topic
			);
				
			return;
			
		}
		
		int c = this.nextCounter(topic);
		
		broadcast.getView(topic).remove(match);
		
		BroadcastMessage broadcastMessage = new BroadcastMessage(
			MessageType.UNS,
			node, 
			topic,
			c,
			null
		);
			
		logger.out(Logger.INFO, node.getID(), "UNSUBSCRIBE; TOPIC \"%s\";\n", topic);
						
		broadcast.broadcast(node, broadcastMessage, lowerID);

		oracle.getStats("UNSUBSCRIPTIONS").add(1);	
		
	}
	
	protected Integer nextCounter(String topic) {
		
		Integer old = this.counter.get(topic);
		
		if (old == null) {
			
			this.counter.put(topic, 1);
			
			return 0;
			
		} else {
			
			this.counter.put(topic, old + 1);
			
			return old;
			
		}	
		
	}
	
	private int getTopicRoot(String topic) {
		
		try {		
			return Integer.parseInt(topic.substring(topic.length() - 3));
		} catch (NumberFormatException e) {
			return 0;
		}
		
	}
	
	@Override
	public Object clone() {
		
		TopicPublishSubscribe object = null;
		
		try { 
			
			object = (TopicPublishSubscribe) super.clone(); 
			object.counter = new HashMap<String, Integer>();
			
		} catch(CloneNotSupportedException e) {}
		
		return object;
	}

}
