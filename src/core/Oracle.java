package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import peersim.core.CommonState;
import peersim.core.Network;

/* A simple singleton for us to keep track 
 * of what's going on with the numbers ;)
 */

public class Oracle {
	
	private static final Oracle instance = new Oracle();
	private int dimension = -1;
	private String[][] clusters = null;
	private int cycleSteps = 0;
	private int applicationCycle = 0;
	private int broadcastCycle = 0;
	private int topologyCycle = 0;
	private long lastMembershipUpdate = 0;
	private boolean fastSubscription = false;
	private String applicationType = null;
	private int[] counterDeliveries;
	private double threshold = 0;
	
	private Map<String, Stats> stats = new HashMap<String, Stats>();
	private Random random = CommonState.r;
	
	private Oracle() {}
	
	public static Oracle getInstance() {		
		return instance;		
	}	
	
	public Random getRandom() {
		return this.random;
	}
	
	public int getDimension() {

		if (this.dimension == -1) {
			
			this.dimension = (int) (Math.log10(Network.size()) / Math.log10(2));
			
		}
		
		return this.dimension;
	}
	
	public void setCluster(String string, int i, int s) {
		
		if (this.clusters == null) {
		
			this.clusters = new String[Network.size()][this.dimension];
			
		}
		
		this.clusters[i][s - 1] = new String(string);
	}
	
	public String[][] getClusters() {
		return this.clusters;
	}

	public int getCycleSteps() {
		return this.cycleSteps;
	}

	public void setCycleSteps(int cycleSteps) {
		this.cycleSteps = cycleSteps;
	}

	public long getLastMembershipUpdate() {
		return this.lastMembershipUpdate;
	}

	public void setLastMembershipUpdate(long lastMembershipUpdate) {
		this.lastMembershipUpdate = lastMembershipUpdate;
	}
	
	
	public int getApplicationCycle() {
		return applicationCycle;
	}

	public void setApplicationCycle(int applicationCycle) {
		this.applicationCycle = applicationCycle;
	}

	public int getBroadcastCycle() {
		return this.broadcastCycle;
	}

	public void setBroadcastCycle(int broadcastCycle) {
		this.broadcastCycle = broadcastCycle;
	}

	public int getTopologyCycle() {
		return this.topologyCycle;
	}

	public void setTopologyCycle(int topologyCycle) {
		this.topologyCycle = topologyCycle;
	}

	public String getApplicationType() {
		return this.applicationType;
	}

	public void setApplicationType(String applicationType) {
		this.applicationType = applicationType;
	}	
	
	public boolean isFastSubscription() {
		return this.fastSubscription;
	}

	public void setFastSubscription(boolean fastSubscription) {
		this.fastSubscription = fastSubscription;
	}

	public void addDelivery(long nodeID) {
		
		if (this.counterDeliveries == null) {
			this.counterDeliveries = new int[Network.size()];
		}
		
		this.counterDeliveries[(int) nodeID]++;
		
	}
	
	public int[] getConunterDeliveries() {
		
		return this.counterDeliveries;
		
	}	
	
	public double getThreshold() {
		return this.threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	public Stats getStats(String key) {		
		
		if (! this.stats.containsKey(key)) {
			this.stats.put(key, new Stats());
		}
		
		return this.stats.get(key);
		
	}
	
	public Map<String, Stats> getStatsSet() {
		return this.stats;
	}
	
}
