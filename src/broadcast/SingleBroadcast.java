package broadcast;

import java.util.ArrayList;
import java.util.List;

import broadcast.message.ACKMessage;
import broadcast.message.AbstractBroadcastMessage;
import broadcast.message.BroadcastMessage;
import broadcast.message.DelegatedBroacastMessage;
import broadcast.message.MessageType;
import broadcast.message.TreeMessage;
import logger.Logger;
import peersim.core.Network;
import peersim.core.Node;

public class SingleBroadcast extends AbsctractCausalBroadcast {
	
	public SingleBroadcast(String prefix) {
		
		super(prefix);
		
		singleRoot = true;
		
	}
	
	@Override
	public void broadcast(Node processI, BroadcastMessage m, int protocolID) {
	
		if ((int) processI.getID() == m.getTopicRoot()) {			
		
			this.doBroadcast(processI, m, true, protocolID);
			
		} else {
			
			logger.out(Logger.INFO, processI.getID(), "DELEGATE; MESSAGE: %s;\n", 
				m.toString()
			);
			
			Node topicRoot = Network.get(m.getTopicRoot());
			
			this.send(processI, topicRoot, 
				new DelegatedBroacastMessage(processI, m), protocolID
			);
			
		}
		
	}
	
}
