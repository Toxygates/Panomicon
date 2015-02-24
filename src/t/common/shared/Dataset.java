package t.common.shared;

import java.util.Date;

public class Dataset extends ManagedItem {

	public Dataset() { }
	
	private String description;
	
	public Dataset(String title, String description, String comment, Date date) {
		super(title, comment, date);
		this.description = description;
	}
	
	public String getDescription() { return description; }
}
