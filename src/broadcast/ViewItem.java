package broadcast;

public class ViewItem {
	
	public enum Action {
        LEAVE, JOIN, FORWARD;
	}
	
	private Integer n;
	private Action o;
	private Integer rc;	

	public ViewItem(Integer n, Action o, Integer rc) {
		this.n = n;
		this.o = o;
		this.rc = rc;
	}
	
	public Integer getN() {
		return this.n;
	}
	
	public Action getO() {
		return this.o;
	}
	
	public Integer getRc() {
		return this.rc;
	}
	
	@Override
	public String toString() {
		return String.format("<%d, %s, %d>", this.n, this.o, this.rc);
	}

}
