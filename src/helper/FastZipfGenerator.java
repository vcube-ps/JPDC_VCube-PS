package helper;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class FastZipfGenerator extends Random {
	
	private Random random = new Random(System.currentTimeMillis());
	
	private NavigableMap<Double, Integer> map;

	public FastZipfGenerator(Random random, int size, double skew) {
		this.map = computeMap(size, skew);
		this.random = random;
	}

	private static NavigableMap<Double, Integer> computeMap(int size, double skew) {
	
		NavigableMap<Double, Integer> map =	new TreeMap<Double, Integer>();
	
		double div = 0;
		
		for (int i = 1; i <= size; i++) {
			div += (1 / Math.pow(i, skew));
		}
	
		double sum = 0;
		
		for(int i=1; i<=size; i++) {
			double p = (1.0d / Math.pow(i, skew)) / div;
			sum += p;
			map.put(sum,  i-1);
		}
		
		return map;
	}

	public int nextZipf() {
		double value = this.random.nextDouble();
		return this.map.ceilingEntry(value).getValue()+1;
	}
	
	

}
