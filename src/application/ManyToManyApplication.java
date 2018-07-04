package application;

import java.util.ArrayList;
import java.util.List;

import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import pubsub.PublishSubscribe;

public class ManyToManyApplication extends AbsctractApplication  {

	private static final String PAR_NUMBER_TOPICS = "number_topics";
	private static final String PAR_PERCENTAGE_SUBSCRIBERS = "percentage_subscribers";
	private static final String PAR_PERCENTAGE_PUBLISHERS = "percentage_publishers";

	private static int numberTopics;
	private static double percentageSubscribers;
	private static double percentagePublishers;
		
	private List<Integer> subscribers = new ArrayList<Integer>();
	private List<Integer> publishers = new ArrayList<Integer>();
	
	private static String topic;

	private boolean sent = false;
	
	public ManyToManyApplication(String prefix) {
		
		super(prefix);

		numberTopics = Configuration.getInt(prefix + "." + PAR_NUMBER_TOPICS, 1);
		percentageSubscribers = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_SUBSCRIBERS, 1);
		percentagePublishers = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_PUBLISHERS, (1.0 / Network.size()));
		
		if (numberTopics < 0) {
			numberTopics = 1;
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
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (node.getID() == 0 && CommonState.getTime() < oracle.getCycleSteps()) {
			
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
			CommonState.getTime() >= (
			((oracle.isFastSubscription()) ? 1 : oracle.getDimension() + 1)) * oracle.getCycleSteps() 
			&& this.publishers.contains((int) node.getID()) && (! this.sent))
		{
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.publish(
				node, 
				topic,
				"one_to_many_" + node.getID()
			);
			
			this.sent = true;
			
		}		

	}

}
