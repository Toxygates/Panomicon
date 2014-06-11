package t.admin.shared;

import java.io.Serializable;

public class Progress implements Serializable {

	public Progress() { }
	public Progress(String task, int percentage) {
		this.task = task;
		this.percentage = percentage;
	}
	
	private String task;
	private int percentage;
	private String[] messages = new String[0];
	
	public void setMessages(String[] messages) {
		this.messages = messages;
	}
	
	public String getTask() { return task; }
	public int getPercentage() { return percentage; }
	public String[] getMessages() { return messages; }
}
