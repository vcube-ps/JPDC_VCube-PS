package application.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.opencsv.CSVReader;

import application.event.BroadcastEvent;
import application.event.SubscribeEvent;
import application.event.UnsubscribeEvent;
import core.Oracle;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class CSVFromFile implements Control {
	
	private static final String PAR_INPUT_FILE = "INPUT_FILE";
	private static final String PAR_PROTOCOL = "protocol";
	
	private static Oracle oracle = Oracle.getInstance();
	
	private final String fileName;
	private final Integer protocolID;
	private final int stepsPerCycle = Oracle.getInstance().getCycleSteps();
	
	private boolean useNext;
	private CSVReader reader;
	private String[] operation;
	private Iterator<String[]> iteratorReader = null;

	public CSVFromFile(String prefix) {
		
		this.fileName = Configuration.getString(PAR_INPUT_FILE, null);
		this.protocolID = Configuration.getPid(prefix + "." + PAR_PROTOCOL);
		
		this.useNext = true;
		
		if (this.fileName == null) {
			return;
		}
		
		try {
			
			this.reader = new CSVReader(
				Files.newBufferedReader(Paths.get(this.fileName)), 
				';'
			);
			
			this.iteratorReader = this.reader.iterator();
			
		} catch (IOException e) {
			
			System.err.printf("\n!!!!! [%s] %s -> %s !!!!!\n\n", 
				this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
			
		}

	}

	@Override
	public boolean execute() {	
		
		if (this.iteratorReader == null) {
			return false;
		}
		
		while(this.iteratorReader.hasNext() || (! this.useNext)) {
			
			if (this.useNext) {
				this.operation = this.iteratorReader.next();
			}
			
			if (this.operation[0].equals("END")) {
				return false;
			}
			
			if (this.operation.length < 2 || this.operation[0].startsWith("#")) {
				
				this.useNext = true;
				
				continue;
				
			}			
			
			Long recordTime = Long.valueOf(this.operation[0]);
			Long simulationTime = CommonState.getTime();			
			Long nextCycle = CommonState.getTime() + this.stepsPerCycle;			
			
			if (recordTime >= simulationTime && recordTime < nextCycle) {
				
				this.useNext = true;
				
				List<Node> nodes = new ArrayList<Node>(); 
				
				if (this.operation[2].equals("*")) {
					
					for (int i = 0; i < Network.size(); i++) {
					
						nodes.add(Network.get(i));
						
					}
					
				} else {
					
					nodes.add(Network.get(Integer.valueOf(this.operation[2])));
					
				}
								
				long eventTime = recordTime - simulationTime;
				
				for (Node node : nodes) {
				
					switch(this.operation[1]) {
					
						case "PUB": 						
							
							EDSimulator.add(
								eventTime,  
								new BroadcastEvent(
									this.operation[4],
									this.operation[3]
								), 		
								node, 
								this.protocolID
							);
							
						break;
						
						case "SUB":
							
							EDSimulator.add(eventTime,
								new SubscribeEvent(
									this.operation[3]
								), 		
								node, 
								this.protocolID
							);
							
						break;
						
						case "UNS":
							
							EDSimulator.add(eventTime,
								new UnsubscribeEvent(
									this.operation[3]
								), 		
								node, 
								this.protocolID
							);
						
						default:						
							
					}
					
				}
				
			} else {
				
				this.useNext = false;
				
				return false; 
				
			}
			
		}		
		
		return false;
	}

}
