package application;

import java.util.ArrayList;
import java.util.List;

import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import pubsub.PublishSubscribe;

public class DynamicOneToManyApplication extends AbsctractApplication  {

	private static final String PAR_PERCENTAGE_SUBSCRIBERS = "percentage_subscribers";
	private static final String PAR_PERCENTAGE_CHURN = "percentage_churn";
	private static final String PAR_NUMBER_MESSAGES = "number_messages";
	private static final String PAR_FACTOR_CHURN = "factor_churn";

	private static double percentageSubscribers;
	private static double percentageChurn;
	private static int numberMessages;
	private static int factorChurn;
		
	private List<Integer> subscribers = new ArrayList<Integer>();
	
	private static String topic;
	private static Integer publisherID;

	private static boolean sent = false;
	private static int sentMessages = 0;
	private static int cycle = 0;
	
	public DynamicOneToManyApplication(String prefix) {
		
		super(prefix);

		percentageSubscribers = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_SUBSCRIBERS, 1);
		percentageChurn = Configuration.getDouble(prefix + "." + PAR_PERCENTAGE_CHURN, 0);
		numberMessages = Configuration.getInt(prefix + "." + PAR_NUMBER_MESSAGES, 1);
		factorChurn = Configuration.getInt(prefix + "." + PAR_FACTOR_CHURN, 3);
		
		percentageSubscribers = (percentageSubscribers > 1) 
			? 1 
			: ((percentageSubscribers < 0) ? 0 : percentageSubscribers);
		
		percentageChurn = (percentageChurn > 1) 
				? 1 
				: ((percentageChurn < 0) ? 0 : percentageChurn);
		
		topic = (percentageSubscribers == 1) ? "ALL" : "RANDOM_" + ((int) (100 * percentageSubscribers));
		
		oracle.setApplicationType(topic);
		
		oracle.getStats("TOPICS").add(1);
		oracle.getStats("PUBLISHERS").add(1);
		oracle.getStats("SUBSCRIBERS").add(percentageSubscribers * Network.size());
		
		publisherID = oracle.getRandom().nextInt(Network.size());
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (node.getID() == 0 && CommonState.getTime() < oracle.getApplicationCycle()) {
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			this.subscribers.add(publisherID);
			
			while (this.subscribers.size() < percentageSubscribers * Network.size()) {

				int rand = oracle.getRandom().nextInt(Network.size());
				
				if (! this.subscribers.contains(rand)) {
					
					this.subscribers.add(rand);
					
				}
				
			}
			
			for (Integer subscriber: this.subscribers) {
				
				pubsub.subscribe(Network.get(subscriber), topic);
				
			}
			
			logger.out(Logger.ALWAYS, node.getID(), "INFO; INIT; NEXT_PUBLIHSER %s;\n", 
				publisherID
			);
					
			logger.out(Logger.ALWAYS, node.getID(), "INFO; INIT; SUBSCRIBED %s;\n", 
				this.subscribers
			);	
			
		} else if (node.getID() == publisherID 
			&& CommonState.getTime() > 20 * oracle.getCycleSteps() 
			&& sentMessages < numberMessages
		) {
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			pubsub.publish(
				node, 
				topic,
				"one_to_many_m_" + sentMessages++ + "_node_" + node.getID()
			);
			
			do {
				
				publisherID = oracle.getRandom().nextInt(Network.size());

			} while (! this.subscribers.contains(publisherID));

			logger.out(Logger.ALWAYS, node.getID(), "INFO; NEXT_PUBLIHSER %s;\n", 
				publisherID
			);
			
		} else if(node.getID() == 0 && 
			CommonState.getTime() > 20 * oracle.getCycleSteps() &&
			((int) (CommonState.getTime() / oracle.getCycleSteps())) % factorChurn == 0
			&& sentMessages < numberMessages) {
			
			logger.err(Logger.ALWAYS, node.getID(), "INFO; CHURN; %.2f;\n", 
				percentageChurn
			);	

			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);			

			if (percentageSubscribers < 1) {
				
				List<Integer> unsubscribers = new ArrayList<Integer>();
				List<Integer> newSubscribers = new ArrayList<Integer>();
				
				for (int i = 0; i < percentageChurn * this.subscribers.size(); i++) {
					
					Integer unsubNode;
					
					do {
						
						unsubNode = this.subscribers.get(oracle.getRandom().nextInt(this.subscribers.size()));
						
					} while (unsubNode == publisherID);
					
					unsubscribers.add(unsubNode);
					
				}
				
				logger.err(Logger.ALWAYS, node.getID(), "INFO; CHURN; LEAVE; %s\n", 
					unsubscribers
				);
				
				for (int i = 0; i < percentageChurn * this.subscribers.size(); i++) {
					
					int subNode;
					
					do {
						
						subNode = oracle.getRandom().nextInt(Network.size());

					} while (this.subscribers.contains(subNode) || unsubscribers.contains(subNode));
					
					newSubscribers.add(subNode);
					
				}
				
				logger.err(Logger.ALWAYS, node.getID(), "INFO; CHURN; JOIN; %s\n", 
					newSubscribers
				);
				
				for (Integer unsubNode : unsubscribers) {
				
					this.subscribers.remove(unsubNode);
					pubsub.unsubscribe(Network.get(unsubNode), topic);
					
				}
				
				for (int subNode : newSubscribers) {

					this.subscribers.add(subNode);
					pubsub.subscribe(Network.get(subNode), topic);
					
				}
				
			}
			
		}
		
			
	}

}
