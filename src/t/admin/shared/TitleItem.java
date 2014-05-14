package t.admin.shared;

import java.io.Serializable;

public abstract class TitleItem implements Serializable {

	protected String title;
	
	public TitleItem() { }
	
	public TitleItem(String title) {
		this.title = title;
	}

	public String getTitle() { return title; }

}
