package core;

import peersim.util.IncrementalStats;

public class Stats extends IncrementalStats {
	
	@Override
	public String toString() {
		
		return String.format(
			"; %d; %.3f; %.3f; %.3f; %.3f;",
			this.getN(), this.getMin(), this.getMax(), 
			this.getAverage(), this.getStD()
		);
		
	}
	
	

}
