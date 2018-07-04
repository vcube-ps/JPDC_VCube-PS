package transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.opencsv.CSVReader;

import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

public final class CustomLatencyTransport implements Transport {

	private static final String PAR_LOGGER_LEVEL = "logger_level";
	private static final String PAR_INPUT_FILE = "matrix";
	private static final String PAR_AVG = "avg";	
	private static final String PAR_STDEV= "stdev";	
		
	private static long avg;		
	private static double stdev;
	private static String fileName;
	
	private static int loggerLevel;
	private static Logger logger;
	private static NormalDistribution normalDistritubtion;
	
	private class FromTo {
		
		public long from;
		public long to;
		
		public FromTo(long from, long to) {
			this.from = from;
			this.to = to;
		}
		
		@Override
		public String toString() {		
			return String.format("[%d,%d]", this.from, this.to);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (this.from ^ (this.from >>> 32));
			result = prime * result + (int) (this.to ^ (this.to >>> 32));
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
			if (!(obj instanceof FromTo)) {
				return false;
			}
			FromTo other = (FromTo) obj;
			if (!this.getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (this.from != other.from) {
				return false;
			}
			if (this.to != other.to) {
				return false;
			}
			return true;
		}

		private CustomLatencyTransport getOuterType() {
			return CustomLatencyTransport.this;
		}	
		
	}
	
	private static Map<FromTo, Long> latencyMatrix = new HashMap<FromTo, Long>();
	
	public CustomLatencyTransport(String prefix) {
		
		CustomLatencyTransport.loggerLevel = Configuration.getInt(prefix + "." + PAR_LOGGER_LEVEL, Logger.INFO);		
		CustomLatencyTransport.fileName = Configuration.getString(prefix + "." + PAR_INPUT_FILE, null);
		CustomLatencyTransport.avg = Configuration.getLong(prefix + "." + PAR_AVG);
		CustomLatencyTransport.stdev = Configuration.getDouble(prefix + "." + PAR_STDEV);		
		
		logger = new Logger(this.getClass().getName(), loggerLevel);
		
		normalDistritubtion = new NormalDistribution(avg, stdev);
		normalDistritubtion.reseedRandomGenerator(CommonState.r.getLastSeed());
		
		if (fileName == null) {
			return;
		}
		
		CSVReader reader;
		String[] operation;
		Iterator<String[]> iteratorReader = null;
		
		try {
			
			reader = new CSVReader(
				Files.newBufferedReader(Paths.get(fileName)), 
				';'
			);
			
			iteratorReader = reader.iterator();
			
			if (iteratorReader == null) {
				return;
			}
			
			long from;
			long to;
			long latency;
			
			while(iteratorReader.hasNext()) {
				
				operation = iteratorReader.next();
				
				from = Long.parseLong(operation[0]);
				to = Long.parseLong(operation[1]);
				latency = (long) Float.parseFloat(operation[2]);
				
				if (from >= Network.size() || to >= Network.size()) {
					
					continue;
					
				}
				
				CustomLatencyTransport.latencyMatrix.put(new FromTo(from, to), latency);
				
			}
			
			
			
		} catch (IOException e) {
			
			System.err.printf("\n!!!!! [%s] %s -> %s !!!!!\n\n", 
				this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
			
		}	
		
	}
	
	@Override
	public void send(Node src, Node dest, Object msg, int pid) {
		
		Long delay = CustomLatencyTransport.latencyMatrix.get(new FromTo(src.getID(), dest.getID()));

		if (delay == null) {
			
			do {
	
				delay = (long) normalDistritubtion.sample();
				
			} while (delay < 0);
			
			logger.err(Logger.DEBUG, src.getID(), 
				"DEBUG; RANDOM_DELAY; TO %d; DELAY %d\n", 
				dest.getID(), delay
			);
			
		} 
		
		EDSimulator.add(delay, msg, dest, pid);
		
	}
	
	/**
	 * Returns a random latency between two nodes.
	 */
	@Override
	public long getLatency(Node src, Node dest) {
		return (long) normalDistritubtion.sample();
	}

	/**
	 * No need to have several instances since here it
	 * is not related to only one node,
	 */
	@Override
	public Object clone() {
		return this;
	}	
	
}
