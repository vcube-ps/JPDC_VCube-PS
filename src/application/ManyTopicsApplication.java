package application;

import helper.FastZipfGenerator;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import pubsub.PublishSubscribe;

public class ManyTopicsApplication extends AbsctractApplication  {

	private static final String PAR_NUMBER_TOPICS = "number_topics";
	private static final String PAR_NUMBER_MESSAGES = "number_messages";
	private static final String PAR_ZIPF = "zipf";
	private static final String PAR_SKEW = "skew";

	private static int numberTopics;
	private static int numberMessages;
	private static boolean zipf;
	private static double skew;
	
	private static FastZipfGenerator zipfGenerator;

	private static int sentMessages = 0;
	
	public ManyTopicsApplication(String prefix) {
		
		super(prefix);

		numberTopics = Configuration.getInt(prefix + "." + PAR_NUMBER_TOPICS, 1);
		numberMessages = Configuration.getInt(prefix + "." + PAR_NUMBER_MESSAGES, Network.size());
		zipf = Configuration.getBoolean(prefix + "." + PAR_ZIPF, false);
		skew = Configuration.getDouble(prefix + "." + PAR_SKEW, 2);
		
		if (numberTopics < 0) {
			numberTopics = 1;
		}
		
		oracle.setApplicationType(String.format("T_%d", numberTopics));
		
		oracle.getStats("TOPICS").add(numberTopics);
		
		if (zipf) {
			
			zipfGenerator = new FastZipfGenerator(oracle.getRandom(), numberTopics, skew);
			
		}
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (node.getID() == 0 && CommonState.getTime() < oracle.getCycleSteps()) {
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			for (int i = 0; i < Network.size(); i++) {
				
				for (int t = 0; t < numberTopics; t++) {
					
					pubsub.subscribe(Network.get(i), this.topic(t));
					
				}
				
			}

		} else if (				
			CommonState.getTime() >= (oracle.getDimension() * 2) * oracle.getCycleSteps() 
			&& sentMessages < numberMessages)
		{
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			int rand = (zipf)
				? zipfGenerator.nextZipf()
				: oracle.getRandom().nextInt(numberTopics);
				
			String topic = this.topic(rand);
			
			pubsub.publish(
				node, 
				topic,
				"many_topics_t_" + topic + "_n_" + node.getID()
			);
			
			oracle.getStats("MSG_TOPIC_" + String.format("%03d", rand)).add(1);
			
			sentMessages++;
			
		}
		
			
	}
	
	private String topic(int id) {
		
		return String.format("topic_%03d", id);
		
	}

}
