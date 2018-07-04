package broadcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import broadcast.message.ACKMessage;
import broadcast.message.AbstractBroadcastMessage;
import broadcast.message.BroadcastMessage;
import broadcast.message.DelegatedBroacastMessage;
import broadcast.message.MessageType;
import broadcast.message.TreeMessage;
import core.Oracle;
import logger.Logger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import topology.VCube;

public abstract class AbsctractCausalBroadcast extends VCube implements Broadcast {
	
	private static final String PAR_LOGGER_LEVEL = "logger_level";
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_UPPER = "upper";
	private static final String PAR_ALLOW_FORWARDERS = "allow_forwarders";
	private static final String PAR_ORDER = "order";
	
	protected static int loggerLevel;
	protected static boolean singleRoot = false;
	
	private static int transportID;
	private static int upperID;
	private static int sendInterval;
	private static boolean allowForwarders;
	private static boolean order;

	protected Map<String, List<TreeMessage>> receptions = new HashMap<String, List<TreeMessage>>();
	protected Map<String, List<CausalBarrierItem>> causalBarrierList = new HashMap<String, List<CausalBarrierItem>>();
	protected Map<String, List<CausalBarrierItem>> firstReception = new HashMap<String, List<CausalBarrierItem>>();
	protected Map<String, List<CausalBarrierItem>> deliveries = new HashMap<String, List<CausalBarrierItem>>();

	protected boolean broadcasting = false;
	protected List<ACKItem> ackSet = new ArrayList<ACKItem>();
	protected List<AbstractBroadcastMessage> messages = new ArrayList<AbstractBroadcastMessage>();
	protected List<BroadcastMessage> pendingFIFO = new ArrayList<BroadcastMessage>();
	protected List<AbstractBroadcastMessage> sendingQueue = new ArrayList<AbstractBroadcastMessage>();
	
	protected static Logger logger;
	protected static Oracle oracle = Oracle.getInstance();

	public AbsctractCausalBroadcast(String prefix) {
		
		loggerLevel = Configuration.getInt(prefix + "." + PAR_LOGGER_LEVEL, Logger.INFO);
		transportID = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		upperID = Configuration.getPid(prefix + "." + PAR_UPPER);
		allowForwarders = Configuration.getBoolean(prefix + "." + PAR_ALLOW_FORWARDERS, true);
		order = Configuration.getBoolean(prefix + "." + PAR_ORDER, true);
		
		sendInterval = oracle.getBroadcastCycle();
		
		logger = new Logger(this.getClass().getName(), loggerLevel);
		
		oracle.setApplicationType(
			new String(this.getClass().getSimpleName().replace("Broadcast", "") + "_").toUpperCase()
			+ oracle.getApplicationType()
		);
		
		oracle.getStats("PENDING_FIFO").add(0);
		oracle.getStats("FALSE_POSITIVE").add(0);
		
	}
	
	// Two different method signatures to deal with single/multi tree when 
	// calculating the number of hops
	
	protected void doBroadcast(Node processI, BroadcastMessage m, int protocolID) {
		this.doBroadcast(processI, m, false, protocolID);
	}

	protected void doBroadcast(Node processI, BroadcastMessage m, boolean single, int protocolID) {
		
		if (processI.equals(m.getSource())) {
		
			logger.out(Logger.INFO, processI.getID(), "BROADCAST; MESSAGE: %s;\n", 
				m.toString()
			);
			
		}
		
		if (single) {
			logger.out(Logger.INFO, processI.getID(), "RECEIVE_DELEGATE; MESSAGE: %s;\n", 
				m.toString()
			);			
		}
	
		String t = m.getTopic();
		Integer s = (int) m.getSource().getID();
		Integer c = m.getCounter();	

		List<ACKItem> ackMatches = this.intersectACKSet(
			String.format("<null, [0-9]+, <%d, %s, .*>>", s, t)
		);
		
		if (! ackMatches.isEmpty()) {
			
			oracle.getStats("PENDING_FIFO").add(this.pendingFIFO.size());
			
			this.pendingFIFO.add(m);
			
			logger.err(Logger.INFO, processI.getID(), "ADD_PENDING; BROADCAST; ORDER %d; FROM %d; MESSAGE: %s;\n", 
				this.pendingFIFO.size(), m.getSource().getID(), m.toString()
			);			
		
			return;
			
		}
		
		if (! this.causalBarrierList.containsKey(t)) {
			this.causalBarrierList.put(t, new ArrayList<CausalBarrierItem>());
		}
		
		List<CausalBarrierItem> oldCausalBarrierList = this.causalBarrierList.get(t);		
		
		if (m.getType().equals(MessageType.PUB.name())) {
		
			this.addFirstReception(processI, t, s, c);
			
			// Since in the case of SRPT not necessarily the root of the tree
			// is subscribed to t, we check it here before delivering
			
			if (this.matchView (
				this.view.get(t), new ViewItem((int) processI.getID(), ViewItem.Action.JOIN, null)
			) != null) {			
				this.deliver(processI, new TreeMessage(processI, m, 0), 0, "BROADCAST");
			}
			
			CausalBarrierItem causalBarrierItem = new CausalBarrierItem(s, c);
			
			if (! this.deliveries.containsKey(t)) {
				this.deliveries.put(t, new ArrayList<CausalBarrierItem>());
			}
				
			List<CausalBarrierItem> deliveriesList = this.deliveries.get(t);		
				
			deliveriesList.remove(new CausalBarrierItem(s, c - 1));
			deliveriesList.add(causalBarrierItem);			
						
			List<CausalBarrierItem> newCausalBarrierList = new ArrayList<CausalBarrierItem>();
			
			newCausalBarrierList.add(causalBarrierItem);
			
			this.causalBarrierList.put(t, newCausalBarrierList);
		
		}
		
		String neighborTopic = (m.getType().equals(MessageType.SUB.name())) 
			? null
			: t;
		
		int topicRoot = (this.isSingleRoot()) ? m.getTopicRoot() : s;
		
		List<Integer> ngbI = this.neighborhood(topicRoot, oracle.getDimension(), neighborTopic);
		
		logger.out(Logger.DEBUG, processI.getID(), "BROADCAST; MESSAGE %s; NEIGHBORS_%d(%d, %s) %s;\n", 
			m.toString(), topicRoot, oracle.getDimension(), neighborTopic, ngbI
		);
		
		for (Integer j : ngbI) {			
			
			TreeMessage treeMessage = new TreeMessage(
				processI, m, ((single) ? 2 : 1),
				(m.getType().equals(MessageType.PUB.name()))
					? new ArrayList<CausalBarrierItem>(oldCausalBarrierList)
					: new ArrayList<CausalBarrierItem>()
			);
				
			this.send(processI, Network.get(j), treeMessage, protocolID);			
			
		}
		
		if (ngbI.size() > 0) {
			
			this.ackSet.add(
				new ACKItem(
					null, 
					ngbI.size(), 
					new ACKMessage(processI, t, c, new ArrayList<ViewItem>())
				)
			);	
			
		} else {
			
			logger.err(Logger.INFO, processI.getID(), "FINISHED_BROADCAST; MESSAGE %s\n", m);
		}
		
		if (m.getType().equals(MessageType.UNS.name())) {
			
			if (this.firstReception.containsKey(t)) {
				this.firstReception.get(t).clear();
			}
			
			if (this.deliveries.containsKey(t)) {
				this.deliveries.get(t).clear();
			}
			
			if (this.receptions.containsKey(t)) {
				this.receptions.get(t).clear();
			}
			
			List<AbstractBroadcastMessage> tmpMessages = new ArrayList<AbstractBroadcastMessage>(this.messages); 
			
			for (AbstractBroadcastMessage message : tmpMessages) {
				
				if (message.getTopic().equals(t)) {
					this.messages.remove(message);					
				}
				
			}
			
		}
			
	}
	
	
	@Override
	public void nextCycle(Node node, int protocolID) {
		
		if (this.sendingQueue.size() > 0) {
		
			AbstractBroadcastMessage message = this.sendingQueue.remove(0);
			
			logger.out(Logger.INFO, node.getID(), "PROCESSING_QUEUE; SEND; TYPE %s; TO %d; MESSAGE: %s;\n", 
				((AbstractBroadcastMessage) message.getData()).getType().toUpperCase(), 
				message.getDestination().getID(), message.getData().toString()
			);
			
			this.processSend(node, message.getDestination(), message, protocolID);
			
		}
		
	}
	
	@Override
	public void processEvent(Node processI, int protocolID, Object event) {
		
		if (event instanceof TreeMessage) {
		
			TreeMessage treeMessage = (TreeMessage) event;
			Node processJ = treeMessage.getSource();
			
			AbstractBroadcastMessage m = (AbstractBroadcastMessage) treeMessage.getData();
			Node sourceNode = m.getSource();
			
			Integer s = (int) sourceNode.getID();
			Integer c = m.getCounter();
			String t = m.getTopic();
			String type = m.getType().toUpperCase();
			
			oracle.getStats("RECEIVED_BCAST").add(1);
			oracle.getStats("RECEIVED_BCAST_" + type).add(1);
			
			int i = (int) processI.getID();
			int j = (int) processJ.getID();
			
			long latencyReception = CommonState.getTime() - m.getCreationTime();
			long latencyTransmission = latencyReception - treeMessage.getWaitTime();
			
			oracle.getStats("LATENCY_RECEPTION").add(latencyReception);
			
			if (! type.equals(MessageType.PUB.name())) {
			
				oracle.getStats("LATENCY_RECEPTION_" + type).add(latencyReception);
				
			}
			
			oracle.getStats("LATENCY_TRANSMISSION").add(latencyTransmission);
			oracle.getStats("LATENCY_TRANSMISSION_" + type).add(latencyTransmission);			
			
			String receptionMessage = String.format(
			"RECEIVE; %s; FROM %d; MESSAGE: %s; LATENCY_RECEPTION %d;",  
				type, processJ.getID(), m, latencyReception
			);
			
			if (m.getType().equals(MessageType.PUB.name())) {
				
				receptionMessage = receptionMessage.concat(
					String.format(" CB_LIST: %s; MESSAGE_SIZE: %d", treeMessage.getCBList(), treeMessage.getCBList().size())
				);
				
				treeMessage.setReceptionTime(CommonState.getTime());
				
			}
			
			logger.out(Logger.INFO, processI.getID(), receptionMessage + "\n");
			
			if (! (m instanceof ACKMessage)) {
			
				int cluster = this.cluster(i, j) - 1;
				
				String neighborTopic = (m.getType().equals(MessageType.SUB.name())) 
					? null
					: t;
					
				List<Integer> ngbI = this.neighborhood(i, cluster, neighborTopic);
				
				logger.out(Logger.DEBUG, processI.getID(), "PROCESS_EVENT; NEIGHBORS_%d(%d, %s) %s;\n", 
					i, cluster, neighborTopic, ngbI
				);
				
				if (ngbI.isEmpty()) {
					
					logger.out(Logger.DEBUG, processI.getID(), "PROCESS_EVENT; LEAF; SEND_ACK; TOPIC %s;\n", t);
					
					ACKMessage ackMessage = new ACKMessage(sourceNode, t, c, new ArrayList<ViewItem>());
					
					this.sendACK(processI, processJ, ackMessage, protocolID);
					
				} else {
					
					List<CausalBarrierItem> cbListCopy = new ArrayList<CausalBarrierItem>(
						treeMessage.getCBList()
					);
					
					ACKItem newItem = new ACKItem(
						j, 
						ngbI.size(), 
						new ACKMessage(sourceNode, t, c, new ArrayList<ViewItem>())
					);
					
					this.ackSet.add(newItem);
					
					logger.out(Logger.DEBUG, processI.getID(), 
						"ADD; PENDING_ACK %s;\n", 
						newItem
					);
					
					for (Integer k : ngbI) {					
					
						TreeMessage newMessage = new TreeMessage(
							processI, m, treeMessage.getHops() + 1,
							treeMessage.getWaitTime(),
							new ArrayList<CausalBarrierItem>(cbListCopy)
						);
						
						this.send(processI, Network.get(k), 
							newMessage, protocolID
						);
						
					}
					
				}
				
			} else {
				
				ACKItem match = null;
			
				for (ACKItem item : this.ackSet) {
					
					ACKMessage ack = item.m;
					
					if (ack.getSource().equals(sourceNode) 
						&& ack.getCounter().equals(c) 
						&& ack.getTopic().equals(t)) {
						
						match = item;
						
						break;
						
					}			
					
				}

				if (match != null) {
				
					this.ackSet.remove(match);
									
					ACKMessage newAck = new ACKMessage(sourceNode, t, c, new ArrayList<ViewItem>(match.m.getData()));
					
					newAck.addData(new ArrayList<ViewItem>(((ACKMessage)m).getData()));
					
					if (match.nb > 1) {
						
						ACKItem newAckItem = new ACKItem(match.j, match.nb - 1, newAck);
						
						this.ackSet.add(newAckItem);
						
					} else { 
						
						if (match.j != null) {
						 
							this.sendACK(processI, Network.get(match.j), newAck, protocolID);
							
						} else {
							
							logger.err(Logger.INFO, processI.getID(), "FINISHED_BROADCAST; MESSAGE %s;\n", m);
							
							if (this.pendingFIFO.size() > 0) {
								
								BroadcastMessage nextMessage = this.pendingFIFO.remove(0);
				
								oracle.getStats("PENDING_FIFO").add(this.pendingFIFO.size());
								
								logger.err(Logger.INFO, processI.getID(), "PROCESS_EVENT; NEXT_PENDING; MESSAGE %s;\n", 
									nextMessage
								);
								
								logger.flush();		
							
								this.broadcast(
									processI, 
									nextMessage, 
									protocolID
								);
								
							}							
							
						}
						
					}

				}
				
			}
			
			if (this.matchView(this.view.get(t), new ViewItem(i, ViewItem.Action.JOIN, null)) != null) {
				
				if (m.getType().equals(MessageType.PUB.name())) {
					
					oracle.getStats("LATENCY_RECEPTION_" + type).add(latencyReception);
					
					this.addFirstReception(processI, t, s, c);
					
					if (! this.receptions.containsKey(t)) {
						this.receptions.put(t, new ArrayList<TreeMessage>());
					}
					
					if (! this.deliveries.containsKey(t)) {				
						this.deliveries.put(t, new ArrayList<CausalBarrierItem>());
					}
					
					this.receptions.get(t).add(treeMessage);
					this.messages.add(m);					

					this.checkReceptions(processI, t);

				} else {
										
					List<ViewItem> viewT = this.view.get(t);			
					List<ViewItem> tmpList;
					
					if (m instanceof ACKMessage) {
						
						tmpList = ((ACKMessage) m).getData();
						
					} else {
						
						if (m.getType().equals(MessageType.UNS.name())) {
							
							if (this.firstReception.get(t) != null) { 
								
								Iterator<CausalBarrierItem> iteratorFirst = firstReception.get(t).iterator();
								
								while (iteratorFirst.hasNext()) {
									
									CausalBarrierItem item = iteratorFirst.next();
								
									if (item.getSource() == s) {
										
										logger.err(Logger.DEBUG, processI.getID(), "PROCESS_EVENT; REMOVE_FIRST; FROM %d;\n", 
											m.getSource().getID() 
										);
										
										iteratorFirst.remove();
										
									}
									
								}
								
							}
							
						}
						
						tmpList = new ArrayList<ViewItem>();
						
						tmpList.add(
							new ViewItem(
								(int) m.getSource().getID(),
								(m.getType().equals(MessageType.SUB.name()))
									? ViewItem.Action.JOIN
									: ViewItem.Action.LEAVE,
								m.getCounter()
							)
						);
						
					}					
					
					this.view.put(t, this.update(viewT, tmpList));
										
				}
				
			} else {
				
				if (m.getType().equals(MessageType.PUB.name())) {
					
					oracle.getStats("LATENCY_RECEPTION_FP").add(latencyReception);
					
					oracle.getStats("FALSE_POSITIVE").add(1);
					oracle.getStats("FALSE_POSITIVE_" + i).add(1);
					
					logger.err(Logger.INFO, processI.getID(), "PROCESS_EVENT; FORWARDER; MESSAGE %s;\n", 
						m
					);
					
				}
				
			}
		} else if (event instanceof DelegatedBroacastMessage) {
		
			DelegatedBroacastMessage delegated = (DelegatedBroacastMessage) event;
			
			logger.out(Logger.DEBUG, processI.getID(), 
				"RECEIVE; DELEGATE; FROM %d; MESSAGE: %s;\n", 
				delegated.getSource().getID(), delegated.getData()
			);	
			
			this.broadcast(processI, (BroadcastMessage) delegated.getData(), protocolID);
			
		} 
		
	}
	
	protected void sendACK(Node processI, Node processJ, ACKMessage m, int protocolID) {
		
		String t = m.getTopic();
		Integer i = (int) processI.getID();		
		Integer s = (int) m.getSource().getID();
		
		ViewItem viewItem = this.matchView(this.view.get(t), new ViewItem(i, ViewItem.Action.JOIN, null));
		
		if ((viewItem != null) && (this.checkFirstReception(this.firstReception.get(t), s) == false)) {
			
			m.getData().add(viewItem);
			
		}
		
		TreeMessage newMessage = new TreeMessage(processI, m);
		
		this.send(processI, processJ, newMessage, protocolID);
		
	}
	
	protected void send (Node sender, Node destination, AbstractBroadcastMessage message, Integer protocolID) {
		
		message.setDestination(destination);
		String type = ((AbstractBroadcastMessage) message.getData()).getType().toUpperCase();
		
		this.sendingQueue.add(message);
		
		long waitTime = message.getWaitTime() + (this.sendingQueue.size() * AbsctractCausalBroadcast.sendInterval);
		
		message.setWaitTime(waitTime);

		oracle.getStats("WAIT_TIME_" + type).add(waitTime);
		oracle.getStats("SENDING_QUEUE_" + type).add(this.sendingQueue.size());		
		oracle.getStats("SENDING_QUEUE").add(this.sendingQueue.size());		
		
		logger.out(Logger.DEBUG, sender.getID(), "SEND; AT %d; TYPE %s; TO %d; MESSAGE: %s;\n", 
			(this.sendingQueue.size() * AbsctractCausalBroadcast.sendInterval + CommonState.getTime()), type, 
			destination.getID(), message.getData()
		);	

	}
	
	private void processSend(Node sender, Node destination, AbstractBroadcastMessage message, int protocolID) {
		
		oracle.getStats("SENT_BCAST").add(1);
		
		oracle.getStats("SENT_BCAST_" + ((AbstractBroadcastMessage) message.getData()).getType().toUpperCase()).add(1);
		
		Transport transportProtocol = (Transport) sender.getProtocol(transportID);
		
		transportProtocol.send(sender, destination, message, protocolID);		
		
	}	
	
	protected void deliver(Node processI, TreeMessage treeMessage, long latencyDifference, String type) {
		
		BroadcastMessage m = (BroadcastMessage) treeMessage.getData();
		
		CommonState.getTime();
		m.getCreationTime();
		
		logger.out(Logger.INFO, processI.getID(), 
			"DELIVER; %s; FROM %d; MESSAGE: %s; HOPS %d; LATENCY_DIFFERENCE %d; WAIT_TIME %d;\n", 
			type, m.getSource().getID(), m.toString(), treeMessage.getHops(), 
			latencyDifference, treeMessage.getWaitTime()
		);
		
		int hops = treeMessage.getHops();
		
		oracle.getStats("HOPS").add(hops);
		
		if (hops > 0) {

			oracle.getStats("LATENCY_DELIVERY").add((CommonState.getTime() - m.getCreationTime()));
			
		}
		
		EDSimulator.add(0, m, processI, upperID);
		
	}
	
	private boolean checkCausalBarrier(Node processI, String t, List<CausalBarrierItem> cbList) {
		
		if (cbList == null) {
			return true;
		}		
	
		Iterator<CausalBarrierItem> iteratorCB = cbList.iterator();
		
		while (iteratorCB.hasNext()) {
			
			CausalBarrierItem cbItem = iteratorCB.next();
			
			Integer s = cbItem.getSource();
			Integer c = cbItem.getCounter();
			Integer sPrime;
			Integer cPrime;
			
			boolean removeDelivery = false;

			for (CausalBarrierItem deliveryItem : this.deliveries.get(t)) {	
				
				sPrime = deliveryItem.getSource();
				cPrime = deliveryItem.getCounter();
				
				if (s.equals(sPrime) && cPrime >= c) {

					removeDelivery = true;
					
				}					

			}
			
			boolean removeFirst = false;
			
			for (CausalBarrierItem firstItem : this.firstReception.get(t)) {	
				
				sPrime = firstItem.getSource();
				cPrime = firstItem.getCounter();
				
				if (s.equals(sPrime) && cPrime > c) {
					
					removeFirst = true;
					
				}					

			}
			
			if (removeDelivery || removeFirst) {
				
				iteratorCB.remove();

				
			}
			
		}
		
		return (cbList.size() == 0);
		
	}
	
	private void checkReceptions(Node processI, String t) {
		
		Integer deliveredMessages;
		
		do {
			
			deliveredMessages = 0;
			
			BroadcastMessage m;
			
			Integer s;
			Integer c;
			List<CausalBarrierItem> cbList;
			
			Iterator<TreeMessage> iteratorRec = this.receptions.get(t).iterator();
			
			while (iteratorRec.hasNext()) {
				
				TreeMessage treeMessage = iteratorRec.next();
				
				m = (BroadcastMessage) treeMessage.getData();
				
				s = (int) m.getSource().getID();
				c = m.getCounter();
				cbList = treeMessage.getCBList();
				
				boolean checkCB = (order) ? this.checkCausalBarrier(processI, t, cbList) : true;
				
				if (checkCB) {
					
					long latencyDifference = CommonState.getTime() - treeMessage.getReceptionTime();
		
					oracle.getStats("LATENCY_DIFFERENCE").add(latencyDifference);
					
					this.deliver(processI, treeMessage, latencyDifference, "CHECK_RECEPTION");
					
					if (! this.causalBarrierList.containsKey(t)) {
						this.causalBarrierList.put(t, new ArrayList<CausalBarrierItem>());
					}
			
					iteratorRec.remove();
					
					this.deliveries.get(t).add(new CausalBarrierItem(s, c));
					
					if (cbList != null) {					
						this.causalBarrierList.get(t).removeAll(cbList);
					}
					
					this.causalBarrierList.get(t).add(new CausalBarrierItem(s, c));
					
					
					this.messages.remove(m);
					
					deliveredMessages++;

				} 
	
			}
			
			logger.flush();
			
			
		} while (deliveredMessages != 0);
		
	}
	
	/* -------------------------------------------------- */
	
	protected List<ACKItem> intersectACKSet(String regex) {
		
		List<ACKItem> matchingSet = new ArrayList<ACKItem>();
		
		for (ACKItem item : this.ackSet) {
			
			if (item.toString().matches(regex)) {
				matchingSet.add(item);
			}
			
		}		
		
		return matchingSet;
		
	}
	
	private boolean checkFirstReception(List<CausalBarrierItem> firstTopic, Integer s) {
		
		if (firstTopic != null) {
		
			for (CausalBarrierItem item : firstTopic) {
				
				if (item.getSource() == s) {
					
					return true;
					
				}
				
			}
			
		}
		
		return false;
		
	}
	
	protected void addFirstReception(Node processI, String t, Integer s, Integer c) {
		
		boolean exist = false;
		
		List<CausalBarrierItem> firstTopic = this.firstReception.get(t);
		
		if (firstTopic != null) {
			
			exist = this.checkFirstReception(firstTopic, s);
			
			
		} else {
			
			firstTopic = new ArrayList<CausalBarrierItem>();
			
		}
			
		if (! exist) {
			
			logger.out(Logger.DEBUG, processI.getID(), 
				"ADD; FIRST_RECEPTION; TOPIC %s; SOURCE %d; COUNTER %d;\n", 
				t, s, c
			);
			
			firstTopic.add(new CausalBarrierItem(s, c));
			
			this.firstReception.put(t, firstTopic);
			
		}
			
		
	}
	
	@Override
	public List<ViewItem> getView(String t) {
		
		if (this.view.get(t) == null) {
			
			this.view.put(t, new ArrayList<ViewItem>());
			
		}
		
		return this.view.get(t);
		
	}
	
	@Override 
	public Map<String, List<ViewItem>> getView() {
		return this.view;
	}
	
	@Override
	public boolean isSingleRoot() {
		return this.singleRoot;
	}
	
	@Override
	public boolean allowForwarders() {
		return this.allowForwarders;
	}
	
	@Override
	public Object clone() {
		
		AbsctractCausalBroadcast object = null;
			
		object = (AbsctractCausalBroadcast) super.clone();

		object.ackSet = new ArrayList<ACKItem>();
		object.causalBarrierList = this.causalBarrierList = new HashMap<String, List<CausalBarrierItem>>();
		object.firstReception = new HashMap<String, List<CausalBarrierItem>>();
		object.deliveries = new HashMap<String, List<CausalBarrierItem>>();
		object.messages = new ArrayList<AbstractBroadcastMessage>();
		object.pendingFIFO = new ArrayList<BroadcastMessage>();
		object.sendingQueue = new ArrayList<AbstractBroadcastMessage>();
		object.receptions = new HashMap<String, List<TreeMessage>>();
	
		return object;
		
	}
	
}
