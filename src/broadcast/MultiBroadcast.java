package broadcast;

import java.util.ArrayList;
import java.util.List;

import broadcast.message.ACKMessage;
import broadcast.message.AbstractBroadcastMessage;
import broadcast.message.BroadcastMessage;
import broadcast.message.MessageType;
import broadcast.message.TreeMessage;
import logger.Logger;
import peersim.core.Network;
import peersim.core.Node;

public class MultiBroadcast extends AbsctractCausalBroadcast {
	
	public MultiBroadcast(String prefix) {
		
		super(prefix);
		
		singleRoot = false;
		
	}
	
	@Override
	public void broadcast(Node processI, BroadcastMessage m, int protocolID) {
	
		this.doBroadcast(processI, m, protocolID);
			
	}
	
}
