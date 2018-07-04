package broadcast;

import java.util.List;
import java.util.Map;

import broadcast.message.BroadcastMessage;
import peersim.cdsim.CDProtocol;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import topology.Topology;

public interface Broadcast extends EDProtocol, CDProtocol, Topology {
	
	public void broadcast(Node i, BroadcastMessage m, int protocolID);
	
	public List<ViewItem> getView(String t);
	public Map<String, List<ViewItem>> getView();
	public ViewItem matchView(List<ViewItem> viewSet, ViewItem search);
	public boolean isSingleRoot();
	public boolean allowForwarders();

}
