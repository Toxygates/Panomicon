package t.admin.shared;

import java.util.Date;

public class Platform extends TitleItem implements DataRecord {

	private int numProbes;
	private String comment;
	private Date date;
	
	public Platform() { }
	
	public Platform(String title, int numProbes, String comment, Date date) {
		super(title);
		this.numProbes = numProbes;
		this.comment = comment;
		this.date = date;
	}
	
	public int getNumProbes() {
		return numProbes;
	}
	
	public Date getDate() { return date; }	
	
	public String getComment() { return comment; }

}
