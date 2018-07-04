package broadcast.message;

import peersim.core.Node;

public class DelegatedBroacastMessage extends AbstractBroadcastMessage {
	
	public DelegatedBroacastMessage(Node source, BroadcastMessage broadcastMessage) {
		
		super(null, source, null, null, broadcastMessage);
		
	}
	
	@Override
	public String toString() {
		
		return String.format(
			"<%d, %s>", 
			this.source.getID(), this.getData()
		);	
			
	}

}
