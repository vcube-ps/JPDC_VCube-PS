package broadcast;

import java.util.ArrayList;
import java.util.List;

import broadcast.message.ACKMessage;
import broadcast.message.AbstractBroadcastMessage;
import broadcast.message.BroadcastMessage;
import broadcast.message.MessageType;
import broadcast.message.TreeMessage;
import logger.Logger;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public class OneToAllBroadcast extends AbsctractCausalBroadcast {
	
	public OneToAllBroadcast (String prefix) {
		
		super(prefix);
		
	}
	
	@Override
	public void broadcast(Node processI, BroadcastMessage m, int protocolID) {
	
		if (processI.equals(m.getSource())) {
			
			logger.out(Logger.INFO, processI.getID(), "BROADCAST; MESSAGE: %s;\n", 
				m.toString()
			);
			
		}
		
		this.deliver(processI, new TreeMessage(processI, m, 0), 0, "BROADCAST");
		
		for (int j = 0; j < Network.size(); j++) {
			
			if (j != processI.getID()) {
		
				TreeMessage treeMessage = new TreeMessage(
					processI, m, 1, new ArrayList<CausalBarrierItem>()
				);
					
				this.send(processI, Network.get(j), treeMessage, protocolID);			
				
			}
			
		}
			
	}
	
	@Override
	public void processEvent(Node processI, int protocolID, Object event) {
		
		if (event instanceof TreeMessage) {
			
			TreeMessage treeMessage = (TreeMessage) event;
			Node processJ = treeMessage.getSource();
			
			AbstractBroadcastMessage m = (AbstractBroadcastMessage) treeMessage.getData();
			Node sourceNode = m.getSource();
			
			Integer s = (int) sourceNode.getID();
			Integer c = m.getCounter();
			String t = m.getTopic();
			String type = m.getType().toUpperCase();
			
			oracle.getStats("RECEIVED_BCAST").add(1);
			oracle.getStats("RECEIVED_BCAST_" + type).add(1);
			
			int i = (int) processI.getID();
			int j = (int) processJ.getID();
			
			long latencyReception = CommonState.getTime() - m.getCreationTime();
			long latencyTransmission = latencyReception - treeMessage.getWaitTime();
			
			oracle.getStats("LATENCY_RECEPTION").add(latencyReception);
			
			if (! type.equals(MessageType.PUB.name())) {
			
				oracle.getStats("LATENCY_RECEPTION_" + type).add(latencyReception);
				
			}
			
			oracle.getStats("LATENCY_TRANSMISSION").add(latencyTransmission);
			oracle.getStats("LATENCY_TRANSMISSION_" + type).add(latencyTransmission);			
			
			String receptionMessage = String.format(
			"RECEIVE; %s; FROM %d; MESSAGE: %s; LATENCY_RECEPTION %d;",  
				type, processJ.getID(), m, latencyReception
			);
			
			if (type.equals(MessageType.PUB.name())) {
				
				oracle.getStats("LATENCY_DIFFERENCE").add(0);
				
				this.deliver(processI, treeMessage, 0, "CHECK_RECEPTION");
			
			}
			
		}

	}	
	
}
	