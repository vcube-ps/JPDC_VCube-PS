package broadcast;

import broadcast.message.ACKMessage;

public class ACKItem {
	
	public Integer j;
	public int nb;
	public ACKMessage m;
	
	public ACKItem(Integer j, int nb, ACKMessage m) {
		this.j = j;
		this.nb = nb;
		this.m = m;
	}
	
	@Override
	public String toString() {
		return String.format("<%d, %d, %s>", this.j, this.nb, this.m);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((j == null) ? 0 : j.hashCode());
		result = prime * result + ((m == null) ? 0 : m.hashCode());
		result = prime * result + nb;
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
		if (!(obj instanceof ACKItem)) {
			return false;
		}
		ACKItem other = (ACKItem) obj;
		if (j == null) {
			if (other.j != null) {
				return false;
			}
		} else if (!j.equals(other.j)) {
			return false;
		}
		if (m == null) {
			if (other.m != null) {
				return false;
			}
		} else if (!m.equals(other.m)) {
			return false;
		}
		if (nb != other.nb) {
			return false;
		}
		return true;
	}
	
}