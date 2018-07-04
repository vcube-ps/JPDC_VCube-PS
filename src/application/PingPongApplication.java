package application;

import java.util.ArrayList;
import java.util.List;

import application.event.PingPongEvent;
import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import pubsub.PublishSubscribe;

public class PingPongApplication extends AbsctractApplication  {
	
	private static final String PAR_WAIT_MESSAGE = "wait_message";

	private static int waitMessage;
	
	private static String topic = "PING_PONG";
	
	private int initialReceptionCounter = 0;
	private boolean sent = false;
	private List<Integer> initPublishers = new ArrayList<Integer>();
	
	public PingPongApplication(String prefix) {
		
		super(prefix);
		
		waitMessage = Configuration.getInt(prefix + "." + PAR_WAIT_MESSAGE, 1);
		
		oracle.setApplicationType(topic);
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (node.getID() == 0 && (CommonState.getTime() < oracle.getCycleSteps())) {
			
			PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
			
			int i;
			
			for (i = 0; i < Network.size(); i++) {					
				pubsub.subscribe(Network.get(i), topic);					
			}
			
			i = 0;
			
			while (i < waitMessage) {
				
				int random = oracle.getRandom().nextInt(Network.size());
				
				if (! this.initPublishers.contains(random)) {
					
					this.initPublishers.add(random);
					i++;
					
				}
				
			}
			
			
			
			logger.out(Logger.INFO, node.getID(), "INFO; INIT; PUBLISHERS %s;\n", 
				this.initPublishers
			);	
			
		} else if (
			CommonState.getTime() >= (oracle.getDimension() + 1) * oracle.getCycleSteps() 
			&& this.initPublishers.contains((int) node.getID()) && (! this.sent)
		) {

				this.pingPong(node, topic);
				
				this.sent = true;
				
				logger.out(Logger.INFO, node.getID(), "INFO; INIT;\n", 
					node.getID()
				);	
		
		}
	
	}
	
	@Override
	public void processEvent(Node node, int protocolID, Object event) {
		
		super.processEvent(node, protocolID, event);
		
		if (event instanceof PingPongEvent) {
			
			this.pingPong(node, topic);
			
		}
		
	}
	
	@Override
	protected void receiveMessage(Node node, Object event, int protocolID) {
		
		super.receiveMessage(node, event, protocolID);
		
		this.initialReceptionCounter++;
		
		if ((! this.sent) && (this.initPublishers.contains((int) node.getID()) || this.initialReceptionCounter >= PingPongApplication.waitMessage)) {
						
			long wait = oracle.getRandom().nextInt(Configuration.getInt("MAX_DELAY_BCAST"));			
			
			wait = Math.max(0, wait);
			
			logger.out(Logger.INFO, node.getID(), "W ; PING; T %s; WT %.3f\n", 
				topic, wait
			);
			
			EDSimulator.add(wait, new PingPongEvent(topic), node, protocolID);
			
			this.sent = true;
			
		}
		
	}
	
	private void pingPong(Node node, String topic) {
		
		PublishSubscribe pubsub = (PublishSubscribe) node.getProtocol(lowerID);
		
		pubsub.publish(
			node, 
			topic,
			"ping_pong_" + node.getID()
		);
		
	}

}
