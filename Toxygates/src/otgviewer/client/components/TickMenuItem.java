package otgviewer.client.components;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class TickMenuItem {

	private boolean ticked = false;
	private MenuItem mi;	
	private final String title;
	private final boolean withImage;
	
	public TickMenuItem(String title, boolean initState, boolean withImage) {
		ticked = initState;
		this.withImage = withImage;
		this.title = title;
		 
		mi = new MenuItem(title, true, new Command() {
			@Override
			public void execute() {				
				setState(!ticked);
				stateChange(ticked);
			}
			
		});
		setState(ticked);
	}
	
	public TickMenuItem(MenuBar mb, String title, boolean initState) {
		this(title, initState, true);
		mb.addItem(mi);
	}
	
	public MenuItem menuItem() {
		return mi;
	}
	
	protected boolean getState(boolean state) {
		return ticked;
	}
	
	protected void setState(boolean state) {
		ticked = state;
		setHTML(withImage);				
	}
	
	protected void setHTML(boolean withImage) {
		if (!withImage) {
			mi.setHTML(title);
		} else if (ticked) {
			mi.setHTML("<img src=\"images/tick_16.png\">" + title);
		} else {
			mi.setHTML("<img src=\"images/blank_16.png\">" + title);
		}
	}
	
	public void stateChange(boolean newState) {
		
	}
	
}
