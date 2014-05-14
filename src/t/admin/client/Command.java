package t.admin.client;

abstract class Command {

	private String title;
	Command(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	abstract void run();	
}
