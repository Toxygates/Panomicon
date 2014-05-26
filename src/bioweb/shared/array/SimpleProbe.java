package bioweb.shared.array;

public class SimpleProbe extends Probe {

	protected String id;
	
	public SimpleProbe() {}
	public SimpleProbe(String title, String id) {
		super(title);
		this.id = id;
	}
	
	public String getID() { return id; }
}
