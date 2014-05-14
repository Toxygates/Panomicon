package t.admin.shared;

public class Platform extends TitleItem {

	private int numProbes;
	
	public Platform() { }
	
	public Platform(String title, int numProbes) {
		super(title);
		this.numProbes = numProbes;
	}
	
	public int getNumProbes() {
		return numProbes;
	}

}
