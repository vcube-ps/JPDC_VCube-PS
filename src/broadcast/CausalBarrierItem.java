package broadcast;

public class CausalBarrierItem {
	
	private int source;
	private int counter;

	public CausalBarrierItem(int source, int counter) {
		this.source = source;
		this.counter = counter;
	}
	
	@Override
	public String toString() {
		return String.format("<%d, %d>", this.source, this.counter);
	}
	
	public int getSource() {
		return this.source;
	}

	public int getCounter() {
		return this.counter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + counter;
		result = prime * result + source;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CausalBarrierItem)) {
			return false;
		}
		CausalBarrierItem other = (CausalBarrierItem) obj;
		if (counter != other.counter) {
			return false;
		}
		if (source != other.source) {
			return false;
		}
		return true;
	}
		
}