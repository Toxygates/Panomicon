package t.common.shared.probe;

import java.io.Serializable;

public abstract class Probe implements Serializable {
	protected String title;
	
	public Probe() {}
	
	public Probe(String title) {
		this.title = title;
	}
	
	public String getTitle() { return title; }	
}
