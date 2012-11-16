package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.DataFilter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Part of a sequence of screens in a workflow.
 * Each screen knows what its parent is, and renders a sequence of links to all the parents.
 * @author johan
 *
 */
public class Screen extends DataListenerWidget {

	protected DockPanel dockPanel = new DockPanel();
	private String key; //An identifier string
	private Screen parent;
	private HorizontalPanel horizontalPanel;
	private boolean shown = false;
	private Label viewLabel = new Label();
	private boolean showDataFilter = false;
	private MenuBar menuBar;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	protected ScreenManager manager;
	
	public Screen(Screen parent, String title, String key,  
			boolean showDataFilter, ScreenManager man) {		
		initWidget(dockPanel);
		menuBar = man.getMenuBar();
		manager = man;
		this.showDataFilter = showDataFilter;
		dockPanel.setWidth("100%");		
		this.key = key;
		this.parent = parent;		
		setTitle(title);		
	}
	
	void addParentLinks(final Screen parent) {
		if (parent != null) {
			addParentLinks(parent.parent());
			Label l;
			if (parent != this) {
				l = new Label(parent.getTitle() + " >> ");
				l.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent e) {
						History.newItem(parent.key());						
					}
				});
				l.setStyleName("clickHeading");			
			} else {
				l = new Label(parent.getTitle());
				l.setStyleName("heading");
			}
			horizontalPanel.add(l);
		}
	}
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		if (!shown) {
			VerticalPanel vp = new VerticalPanel();
			horizontalPanel = new HorizontalPanel();
			vp.add(horizontalPanel);
			dockPanel.add(vp, DockPanel.NORTH);
			addParentLinks(this);
			dockPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
			dockPanel.add(content(), DockPanel.CENTER);
			vp.add(viewLabel);			
			shown = true;
//			Window.alert("First show");
		} else {
//			Window.alert("Late show");
		}
		for (MenuItem mi: menuItems) {
			mi.setVisible(true);
		}
		loadState();
	}
	
	/**
	 * This method will be called each time the screen is hidden.
	 * If overriding, make sure to call the superclass method.
	 */
	public void hide() {		
		for (MenuItem mi: menuItems) {
			mi.setVisible(false);
		}
	}
	
	public void addMenu(MenuItem m) {
		menuBar.addItem(m);
		m.setVisible(false);
		menuItems.add(m);
	}
	
	public void resizeInterface(int newHeight) {
		String h = (newHeight - dockPanel.getAbsoluteTop()) + "px";
		dockPanel.setHeight(h);		
		changeHeight(newHeight);
	}
	
	/**
	 * Override this method to define the main content of the screen.
	 * Stored state may not have been loaded when this method is invoked.
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
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		if (showDataFilter) {
			viewLabel.setText("Viewing: " + filter.toString());
		}
	}
}
