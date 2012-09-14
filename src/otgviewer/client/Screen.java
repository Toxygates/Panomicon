package otgviewer.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Part of a sequence of screens in a workflow.
 * Each screen knows what its parent is, and renders a sequence of links to all the parents.
 * @author johan
 *
 */
public class Screen extends DataListenerWidget {

	private DockPanel dockPanel = new DockPanel();
	private String key; //An identifier string
	private Screen parent;
	private HorizontalPanel horizontalPanel;
	private boolean shown = false;
	
	public Screen(Screen parent, String title, String key) {
		initWidget(dockPanel);
	
		dockPanel.setWidth("100%");
		this.key = key;
		this.parent = parent;		
		setTitle(title);
				
	}
	
	public void addParentLinks(final Screen parent) {
		if (parent != null) {
			addParentLinks(parent.parent());
			Label l = new Label(parent.getTitle() + " >> ");
			
			if (parent != this) {
				l.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent e) {
						History.newItem(parent.key());
					}
				});
				l.setStyleName("clickHeading");			
			} else {
				l.setStyleName("heading");
			}
			horizontalPanel.add(l);
		}
	}
	
	public void show() {
		if (!shown) {
			horizontalPanel = new HorizontalPanel();
			dockPanel.add(horizontalPanel, DockPanel.NORTH);
			addParentLinks(this);
			dockPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
			dockPanel.add(content(), DockPanel.CENTER);
			shown = true;
		}
		loadState();
	}
	
	public void resizeInterface(int newHeight) {
		String h = (newHeight - dockPanel.getAbsoluteTop()) + "px";
		dockPanel.setHeight(h);		
	}
	
	/**
	 * Override this method to define the main content of the screen.
	 * @return
	 */
	public Widget content() {
		return new SimplePanel();
	}
	
	public String key() {
		return key;
	}
	
	public Screen parent() {
		return parent;
	}	
	
}
