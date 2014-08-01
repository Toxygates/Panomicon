package t.admin.shared;

import java.io.Serializable;
import java.util.Date;

public abstract class ManagedItem implements Serializable, DataRecord {

	protected String title, comment;
	protected Date date;
	
	public ManagedItem() { }
	
	public ManagedItem(String title, String comment, Date date) {
		this.title = title;
		this.comment = comment;
		this.date = date;
	}

	public String getTitle() { return title; }
	
	public String getComment() { return comment; }
	public void setComment(String c) { comment = c; }
	public Date getDate() { return date; }
}
