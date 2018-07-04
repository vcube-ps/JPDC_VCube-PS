package application;

import java.util.ArrayList;
import java.util.List;

import broadcast.message.BroadcastMessage;
import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import pubsub.PublishSubscribe;

public class DynatopsApplication extends AbsctractApplication  {

	private static final String PAR_NUMBER_MESSAGES = "number_messages";
	private static final String PAR_PER_BROKER = "per_broker";
	private static final String PAR_PERCENTAGE_SUBSCRIBERS = "percentage_subscribers";
	private static final String PAR_PERCENTAGE_PUBLISHERS = "percentage_publishers";
	
	private static int numberMessages;
	private static int perBroker;
	private static double percentageSubscribers;
	private static double percentagePublishers;
	
	// used to simulate the queue for brokers,
	// just like it would be to send the message to each node
	// associated to a broker
	private int brokerWait = 0;
	private int lastSent = 0;
	
	private List<Integer> subscribers = new ArrayList<Integer>();
	private List<Integer> publishers = new ArrayList<Integer>();
		
	private static String topic;

	private boolean sent = false;
	
	public DynatopsApplication(String prefix) {
		
		super(prefix);

		perBroker = Configuration.getInt(prefix + "." + PAR_PER_BROKER, 1);
		numberMessages = Configuration.getInt(prefix + "." + PAR_NUMBER_MESSAGES, 1);
		percentageSubscribers = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_SUBSCRIBERS, 1);
		percentagePublishers = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_PUBLISHERS, (1.0 / Network.size()));
		
		if (numberMessages < 0) {
			numberMessages = 1;
		}
		
		percentageSubscribers = (percentageSubscribers > 1) 
			? 1 
			: ((percentageSubscribers < 0) ? 0 : percentageSubscribers);
		
		percentagePublishers = (percentagePublishers > 1) 
				? 1 
				: ((percentagePublishers < 0) ? 0 : percentagePublishers);
		
		topic = String.format("P%d_S%d", ((int) (100 * percentagePublishers)), ((int) (100 * percentageSubscribers)));
		
		oracle.setApplicationType(topic);
		
		oracle.getStats("TOPICS").add(1);
		oracle.getStats("PUBLISHERS").add(percentagePublishers * Network.size());
		oracle.getStats("SUBSCRIBERS").add(percentageSubscribers * Network.size());
		
		oracle.setFastSubscription(true);
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (node.getID() == 0 && CommonState.getTime() < oracle.getBroadcastCycle()) {
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			while (this.publishers.size() < percentagePublishers * Network.size()) {

				int rand = oracle.getRandom().nextInt(Network.size());
				
				if (! this.publishers.contains(rand)) {
					
					this.publishers.add(rand);
					
				}
				
			}
			
			this.subscribers.addAll(this.publishers);
			
			while (this.subscribers.size() < percentageSubscribers * Network.size()) {

				int rand = oracle.getRandom().nextInt(Network.size());
				
				if (! this.subscribers.contains(rand)) {
					
					this.subscribers.add(rand);
					
				}
				
			}			
			
			for (Integer subscriber: this.subscribers) {
			
				pubsub.subscribe(Network.get(subscriber), topic);
			
			}
			
			logger.out(Logger.ALWAYS, node.getID(), "INFO; INIT; PUBLIHSERS %s;\n", 
					this.publishers
			);
					
			logger.out(Logger.ALWAYS, node.getID(), "INFO; INIT; SUBSCRIBED %s;\n", 
				this.subscribers
			);	
			
		} else if (				
			this.numberMessages > 0 && publishers.contains((int) node.getID()) 
			&& CommonState.getTime() > oracle.getBroadcastCycle() 
			&& (oracle.getRandom().nextInt(100) < 5) && (lastSent == 0 || CommonState.getTime() - 500 > lastSent)				
			)
		{
			
			lastSent = (int) CommonState.getTime();

			this.numberMessages--;
			
			System.err.println("pub time " + CommonState.getTime());
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.publish(
				node, 
				topic,
				"one_to_many_" + node.getID()
			);
			
		} else if (this.brokerWait > 0) {				
				this.brokerWait--;
		}

	}
	
	protected void receiveMessage(Node node, Object event, int protocolID) {		
		
		BroadcastMessage message = (BroadcastMessage) event;

		double latency = CommonState.getTime() - message.getCreationTime();
		
		logger.out(Logger.INFO, node.getID(), "RECEIVE_BROKER; BROADCAST; FROM %d; MESSAGE: %s; DATA \"%s\"; LATENCY_DELIVERY %.3f;\n", 
			message.getSource().getID(), message, message.getData(), 
			latency
		);
		
		oracle.getStats("RECEIVED_BROKER").add(1);
		
		for (int z = 0; z < this.perBroker; z++) {
			
			oracle.getStats("LATENCY_DELIVERY_NODE").add(latency + 100 + this.brokerWait + z);
	
		}
		
		this.brokerWait += this.perBroker;
		
		oracle.addDelivery(node.getID());

	}

}
