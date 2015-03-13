package t.common.client;

public abstract class Command {

	private String title;
	public Command(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	abstract public void run();	
}
