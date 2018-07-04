package topology;

import java.util.List;

import broadcast.ViewItem;

public interface Topology {
	
	public List<Integer> neighborhood(int i, int h, String t);
	public Integer firstFaultFreeNeighbor(int i, int s, String t);
	public int cluster(int i, int j);
	public List<ViewItem> update(List<ViewItem> set1, List<ViewItem> set2);
	
}
