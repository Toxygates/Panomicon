package t.admin.shared;

import java.io.Serializable;

public class Progress implements Serializable {

	public Progress() { }
	public Progress(String task, int percentage, boolean allFinished) {
		this.task = task;
		this.percentage = percentage;
		this.allFinished = allFinished;
	}
	
	private String task;
	private int percentage;
	private String[] messages = new String[0];
	private boolean allFinished;
	
	public void setMessages(String[] messages) {
		this.messages = messages;
	}
	
	public String getTask() { return task; }
	public int getPercentage() { return percentage; }
	public String[] getMessages() { return messages; }
	
	/**
	 * Is every task finished?
	 * @return
	 */
	public boolean isAllFinished() { return allFinished; }
}
