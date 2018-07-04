package broadcast.message;

import java.util.ArrayList;
import java.util.List;

import broadcast.ViewItem;
import peersim.core.Node;

public class ACKMessage extends AbstractBroadcastMessage {
	
	private List<ViewItem> data = new ArrayList<ViewItem>();
	
	public ACKMessage(Node source, String topic, Integer counter, List<ViewItem> data) {
		
		super("ACK", source, topic, counter, null);
		
		this.data = data;
		
	}
	
	public void addData(List<ViewItem> list) {
		
		for (ViewItem item : list) {
		
			if (! this.data.contains(item)) {
				
				this.data.add(item);
			}
		}
		
	}

	@Override
	public List<ViewItem> getData() {
		
		return this.data;
		
	}
	
	@Override
	public String toString() {
		return String.format("<%d, %s, %s>", this.getSource().getID(), this.getTopic(), this.getData());
	}

}
