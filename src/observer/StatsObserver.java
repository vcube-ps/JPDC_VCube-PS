package observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import core.Oracle;
import peersim.config.Configuration;
import peersim.core.Control;

public class StatsObserver implements Control {
	
	public StatsObserver(String prefix) {
		
	}

	@Override
	public boolean execute() {
		
		Oracle oracle = Oracle.getInstance();
		
		int[] counterDeliveries = oracle.getConunterDeliveries();
		
		if (counterDeliveries != null) {
		
			System.out.println("\n# Deliveries to application layer per node: ");	
			
			for (int k =  0; k < counterDeliveries.length; k++) {
				
				System.out.printf("# [%d] Delivered %d\n", k, counterDeliveries[k]);
				
			}
			
			System.out.println();
			
		}
		
		System.out.printf(
			"# Last membership update: %d\n", oracle.getLastMembershipUpdate()	
		);
		
		System.out.println("\nNAME; QUANTITY; MIN; MAX; AVG; STD;");
		
		List<String> keys = new ArrayList<String>(oracle.getStatsSet().keySet());
		
		Collections.sort(keys);
		
		for (String key : keys) {
			
			System.out.printf("%s%s\n", key, oracle.getStats(key));
			
		}	
		
		double memoryMax = Runtime.getRuntime().maxMemory() / (double) (1024 * 1024);
		double memoryFree = Runtime.getRuntime().freeMemory() / (double) (1024 * 1024);
		double memoryTotal = Runtime.getRuntime().totalMemory() / (double) (1024 * 1024);
		
		System.out.printf(
			"\n# APPLICATION: %s\n# BROADCAST: %s\n\n",
			Configuration.getString("protocol.app"),
			Configuration.getString("protocol.broadcast")
			
		);
		
		System.out.printf(
			"\n# MEMORY: MAX %.3f - FREE %.3f - TOTAL %.3f\n",
			memoryMax, memoryFree, memoryTotal
		);
		
		return false;
	}

}
