package pubsub;

import java.util.ArrayList;
import java.util.List;

import peersim.core.Protocol;

public class TopicList implements Protocol {
	
	private List<String> topicList = new ArrayList<String>();
	
	public TopicList(String prefix) {}
	
	public void add(String topic) {
		
		if (! this.topicList.contains(topic)) {
			this.topicList.add(topic);
		}
		
	}
	
	public void remove(String topic) {
		this.topicList.remove(topic);
	}
	
	@Override
	public Object clone() {
		
		TopicList object = null;
		
		try { 
			
			object = (TopicList) super.clone();
			object.topicList = new ArrayList<String>();
			
		} catch(CloneNotSupportedException e) {}
		
		return object;
	}	

}
