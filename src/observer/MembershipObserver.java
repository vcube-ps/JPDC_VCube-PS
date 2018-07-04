package observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import broadcast.Broadcast;
import broadcast.ViewItem;
import core.Oracle;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class MembershipObserver implements Control {
	
	private static final String PAR_TOPOLOGY = "broadcast";
	
	protected static int broadcastID;
	
	public MembershipObserver(String prefix) {
		
		broadcastID = Configuration.getPid(prefix + "." + PAR_TOPOLOGY);
		
	}

	@Override
	public boolean execute() {
		
		Oracle oracle = Oracle.getInstance();
		
		System.out.println("\n# Membership per node: ");
		
		for (int i = 0; i < Network.size(); i++) {
			
			Node node = Network.get(i);
			
			List<String> topics = new ArrayList<String>();
			Map<String, List<Integer>> view = new HashMap<String,List<Integer>>();
					
			Broadcast broadcast = (Broadcast) node.getProtocol(broadcastID);
			
			for (String t : broadcast.getView().keySet()) {
				
				view.put(t, new ArrayList<Integer>());
				
				for (ViewItem item : broadcast.getView().get(t)) {
					
					if (item.getO().equals(ViewItem.Action.JOIN)) {
						
						view.get(t).add(item.getN());
					
						if (item.getN().equals(i)) {
							
							topics.add(t);
							
						}
						
					}
					
				}				
				
			}
			
			System.out.printf("# Node: %d; View: %s;\n", node.getID(), view);
			
		}
 
		return false;
		
	}

}
