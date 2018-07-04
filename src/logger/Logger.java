package logger;

import peersim.core.CommonState;

public class Logger {
	
	public static final Integer OFF = 0;
	public static final Integer ALWAYS = 1;
	public static final Integer INFO = 2;
	public static final Integer DEBUG = 3;
	public static final Integer TEST = 4;
	public static final Integer ALL = 93;
	
	private static final Integer DEFAULT = INFO;
	
	private int printLevel;
	private String prefix;	
	
	
	public Logger(String prefix) {
		 this(prefix, DEFAULT);
	}	
	
	public Logger(String prefix, int printLevel) {
		
		this.prefix = prefix;
		this.printLevel = printLevel;
	}
	
	public void out(int level, Long nodeID, String format, Object ... args) {
		
		if (level <= this.printLevel) {
			
			String out = String.format("[%d];[%s]; N; %d; ",
				CommonState.getTime(),
				this.prefix,
				nodeID
			);
			
			out += String.format(format, args);		
		
			System.out.printf(out);
			
		}
		
	}
	
	public void err(int level, Long nodeID, String format, Object ... args) {
		
		if (level <= this.printLevel) {
		
			String err = String.format("[%d];[%s]; N; %d; ", 
				CommonState.getTime(),
				this.prefix,
				nodeID
			);
				
			err += String.format(format, args);
				
			System.err.printf(err);
			
		}
		
	}
	
	public void err(int level, String format, Object ... args) {
		
		if (level <= this.printLevel) {
		
			String err = String.format("[%d];[%s]; ", 
				CommonState.getTime(),
				this.prefix
			);
				
			err += String.format(format, args);
				
			System.err.printf(err);
			
		}
		
	}
	
	
	public void append(int level, String format, Object ... args) {
		
		if (level <= this.printLevel) {
		
			System.out.printf(format, args);
			
		}
		
	}
	
	public void out(Long nodeID, String format, Object ... args) {
		
		this.out(DEFAULT, nodeID, format, args);

	}
	
	public void err(Long nodeID, String format, Object ... args) {
		
		this.err(DEFAULT, nodeID, format, args);

	}
	
	public void err(String format, Object ... args) {
		
		this.err(DEFAULT, format, args);
		
	}	
	
	public void append(String format, Object ... args) {
		
		this.append(DEFAULT, format, args);
		
	}
	
	public void flush() {
		
		System.out.flush();
		System.err.flush();
		
	}
	
}
