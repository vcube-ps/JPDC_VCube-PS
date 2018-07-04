package topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import broadcast.ViewItem;
import core.Oracle;

public abstract class VCube {
	
	protected Map<String, List<ViewItem>> view = new HashMap<String, List<ViewItem>>();
	
	/*
	 * Returns the index s of the cluster of
	 * process i that contains process j.
	 * 
	 * cluster_i(j) = s (msb(i xor j) + 1)
	 */
	public int cluster(int i, int j) {
		
		int s = 0;
		
		for (int k = i ^ j; k > 0; k = k >> 1) {
			s++;
		}
		
		return s;
		
	} 
	
	/*
	 *  Returns the first fault-free node j in 
	 *  the cluster s of node i (c(i, s))
	 */	
	public Integer firstFaultFreeNeighbor(int i, int s, String t) {
		
		List<Integer> cluster = new LinkedList<Integer>();	
		
		this.cis(cluster, i, s);
		
		if ((t != null) && ! this.view.containsKey(t)) {
			this.view.put(t, new ArrayList<ViewItem>());
		}
				
		do {
			
			int k = cluster.remove(0);
			
			if (t == null) {
				return k;
			}
			
			ViewItem match = null;
			
			if (this.view.containsKey(t)) {
			
				match = this.matchView(this.view.get(t), new ViewItem(k, ViewItem.Action.JOIN, null));
				
				if (match == null) {
					
					match = this.matchView(this.view.get(t), new ViewItem(k, ViewItem.Action.FORWARD, null));
					
				}
				
			}
			
			if (match != null) {
				
				return k;
				
			}
			
		} while (! cluster.isEmpty());
		
		return null;
		
	}
	
	/*
	 * Returns the set of all processes that are virtually 
	 * connected to process i.
	 *  
	 * neighborhood_i(h) = {j | j = FF_neighbor_i(s), 
	 * j != null, 1 <= s <= h, h <= log2(n)}
	 */	
	public List<Integer> neighborhood(int i, int h, String t) {		
		
		List<Integer> neighbors = new ArrayList<Integer>();
		
		for (int s = 1; s <= h; s++) {
			
			Integer firstFaultFreeNeighbor = this.firstFaultFreeNeighbor(i, s, t);
			
			if (firstFaultFreeNeighbor != null) {
				neighbors.add(firstFaultFreeNeighbor);
			}
			
		}
		
		return neighbors;		
		
	}
	
	/*
	 * Determines the cluster tested by node i during the round s
	 *  
	 * c_(i,s) = { i xor 2^(s-1), c_(i xor 2^(s-1), 1), ..., c_(i xor 2^(s-1), (s-1)) } 	
	 */
	private void cis(List<Integer> cluster, int i, int s) {
		
		int xor = i ^ (int) Math.pow(2, (s - 1));
		
		cluster.add(xor);
		
		for (int j = 1; j < s; j++) {
			
			this.cis(cluster, xor, j);
			
		}
		
	}
	
	/*
	 *  It's used to merge two lists of subscriptions. 
	 *  It compares the counters to choose the most recent operation
	 *  of a node n for a group g.
	 */
	public List<ViewItem> update(List<ViewItem> set1, List<ViewItem> set2) {
		
		List<ViewItem> copySet1 = new ArrayList<ViewItem>(set1);
		List<ViewItem> copySet2 = new ArrayList<ViewItem>(set2);
		
		for (ViewItem item1 : set1) {
			
			int n1 = item1.getN();
			int rc1 = item1.getRc();
			
			ViewItem matchSet2 = this.matchView(set2, new ViewItem(n1, null, null));
			
			if (matchSet2 != null) {
				
				int rc2 = matchSet2.getRc();
				
				if (rc2 > rc1) {
					
					ViewItem removeItem = this.matchView(set1, new ViewItem(n1, null, rc1));
					
					copySet1.remove(removeItem);

				} else {
					
					ViewItem removeItem = this.matchView(set2, new ViewItem(n1, null, rc2));
					
					copySet2.remove(removeItem);
					
				}
				
			}			
			
		}
		
		copySet1.addAll(copySet2);
		 
		return copySet1;
		
	}
	
	public ViewItem matchView(List<ViewItem> viewSet, ViewItem search) {
		
		if (viewSet == null) {
			return null;
		}
		
		for (ViewItem item : viewSet) {
			
			if (item.getN().equals(search.getN())) {
				
				if (search.getO() == null && search.getRc() == null) {
					
					return item;
					
				}  else if (search.getO() != null && item.getO().equals(search.getO())) {
					
					return item;
					
				} else if ((search.getRc() != null && item.getRc().equals(search.getRc()))) {
					
					return item;
				}
				
			}
			
		}
		
		return null;
		
	}
	
	@Override
	public Object clone() {
		
		VCube object = null;
		
		try {
			
			object = (VCube) super.clone();
			
			if (! Oracle.getInstance().isFastSubscription()) {
			
				object.view = new HashMap<String, List<ViewItem>>();
				
			}
			
		} catch (CloneNotSupportedException e) {}
		
		return object;
		
	}
	
}
