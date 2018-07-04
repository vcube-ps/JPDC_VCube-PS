package broadcast.message;

import java.util.List;

import broadcast.CausalBarrierItem;
import peersim.core.Node;

public class TreeMessage extends AbstractBroadcastMessage {
	
	private Integer hops = 0;
	private List<CausalBarrierItem> cbList = null;
	private long receptionTime;
	
	public TreeMessage(Node source, Object data, int hops) {
		
		super("TREE", source, null, null, data);
		
		this.hops = hops;
		
	}
	
	public TreeMessage(Node source, Object message) {
		
		this(source, message, 1);
		
	}
	
	public TreeMessage(Node source, Object message, List<CausalBarrierItem> cbList) {
		
		this(source, message, 1, cbList);
		
	}
	
	public TreeMessage(Node source, Object message, int hops, List<CausalBarrierItem> cbList) {
		
		this(source, message, hops);
		
		this.cbList = cbList;
		
	}
	
	public TreeMessage(Node source, Object message, int hops, long waitTime, List<CausalBarrierItem> cbList) {
		
		this(source, message, hops);
		
		this.cbList = cbList;
		this.waitTime = waitTime;
		
	}	
	
	public Integer getHops() {
		return this.hops;
	}

	public List<CausalBarrierItem> getCBList() {
		return this.cbList;
	}

	public void setCBList(List<CausalBarrierItem> cbList) {
		this.cbList = cbList;
	}	
	
	public long getReceptionTime() {
		return this.receptionTime;
	}

	public void setReceptionTime(long receptionTime) {
		this.receptionTime = receptionTime;
	}
	
	@Override
	public String toString() {
		
		return String.format(
			"<%d, %s, %s>", 
			this.getSource().getID(), this.getData(), this.getCBList()
		);	
			
	}
	
}
