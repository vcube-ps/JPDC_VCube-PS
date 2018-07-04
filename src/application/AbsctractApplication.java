package application;

import broadcast.message.BroadcastMessage;
import core.Oracle;
import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public abstract class AbsctractApplication implements Application {
	
	private static final String PAR_LOWER = "lower";
	private static final String PAR_LOGGER_LEVEL = "logger_level";

	protected static int lowerID;
	protected static int loggerLevel;
	
	protected static Logger logger;
	protected static Oracle oracle = Oracle.getInstance();

	protected AbsctractApplication(String prefix) {
	
		lowerID = Configuration.getPid(prefix + "." + PAR_LOWER);
		loggerLevel = Configuration.getInt(prefix + "." + PAR_LOGGER_LEVEL, Logger.INFO);
		
		logger = new Logger(this.getClass().getName(), loggerLevel);
		
		oracle.getStats("NODES").add(Network.size());
		
	}

	@Override
	public void nextCycle(Node node, int protocolID) {}

	@Override
	public void processEvent(Node node, int protocolID, Object event) {
		
		if (event instanceof BroadcastMessage) {
			
			this.receiveMessage(node, event, protocolID);
			
		}

	}
	
	protected void receiveMessage(Node node, Object event, int protocolID) {		
		
		BroadcastMessage message = (BroadcastMessage) event;

		double latency = CommonState.getTime() - message.getCreationTime();
		
		logger.out(Logger.INFO, node.getID(), "RECEIVE; BROADCAST; FROM %d; MESSAGE: %s; DATA \"%s\"; LATENCY_DELIVERY %.3f;\n", 
			message.getSource().getID(), message, message.getData(), 
			latency
		);
		
		oracle.getStats("RECEIVED_APPLICATION").add(1);
		
		oracle.addDelivery(node.getID());
		
	}

	@Override
	public Object clone() {
		
		AbsctractApplication object = null;
		
		try { 
			
			object = (AbsctractApplication) super.clone(); 
			
		} catch(CloneNotSupportedException e) {}
		
		return object;
	}

}
