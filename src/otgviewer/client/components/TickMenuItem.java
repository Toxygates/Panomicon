package otgviewer.client.components;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class TickMenuItem {

	private boolean ticked = false;
	private MenuItem mi;
	
	private final String title;
	
	public TickMenuItem(MenuBar mb, String title, boolean initState) {
		ticked = initState;
		this.title = title;
		 
		mi = new MenuItem(title, true, new Command() {
			@Override
			public void execute() {
				ticked = !ticked;
				setHTML();				
				stateChange(ticked);
			}
			
		});
		setHTML();
		mb.addItem(mi);
	}
	
	private void setHTML() {
		if (ticked) {
			mi.setHTML("<img src=\"images/tick_16.png\">" + title);
		} else {
			mi.setHTML("<img src=\"images/blank_16.png\">" + title);
		}
	}
	
	public void stateChange(boolean newState) {
		
	}
	
}
