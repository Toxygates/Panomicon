package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.Group;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 * @author johan
 *
 */
public class ColumnScreen extends Screen {
	public static String key = "columns";
	
	private GroupInspector gi;
	private CompoundSelector cs;	
	private TabLayoutPanel tp;
	
	public ColumnScreen(ScreenManager man) {
		super("Sample group definitions", key, true, false, man,
				resources.groupDefinitionHTML(), resources.groupDefinitionHelp());
		
		cs = new CompoundSelector(this, "Compounds");
		this.addListener(cs);
		cs.setStyleName("compoundSelector");
	}
	
	@Override
	public boolean enabled() {
		return manager.isConfigured(DatasetScreen.key); 
	}

	
	@Override
	protected void addToolbars() {
		super.addToolbars();
		addLeftbar(cs, 350);
	}

	public Widget content() {				
		tp = new TabLayoutPanel(30, Unit.PX);
		
		gi = new GroupInspector(cs, this);
		this.addListener(gi);
		cs.addListener(gi);
		tp.add(gi, "Sample groups");
		
		final CompoundRanker cr = new CompoundRanker(cs);
//		cr.setSize("500px", "500px");
		tp.add(Utils.makeScrolled(cr), "Compound ranking (optional)");
		tp.selectTab(0);

		return tp;
	}

	@Override
	public Widget bottomContent() {
		HorizontalPanel hp = Utils.mkWidePanel();
		Button b = new Button("Next: Select probes", new ClickHandler() {			
			public void onClick(ClickEvent event) {
				if (gi.chosenColumns().size() == 0) {
					Window.alert("Please define and activate at least one group.");
				} else {
					configuredProceed(ProbeScreen.key);					
				}
			}
		});
		hp.add(b);		
		return hp;
	}
	
	@Override
	public void loadState(Storage s) {
		super.loadState(s);
		if (visible) {
			//If we became visible, we must have been enabled, so can count on a
			//data filter being present.
			try {
				List<Group> ics = loadColumns("inactiveColumns", 
						new ArrayList<BarcodeColumn>(gi.existingGroupsTable.inverseSelection()));
				if (ics != null) {
					gi.inactiveColumnsChanged(ics);
				}

			} catch (Exception e) {
				Window.alert("Unable to load inactive columns.");
			}
		}
	}

	@Override
	public void tryConfigure() {
		if (chosenColumns.size() > 0) {		
			setConfigured(true);
		}
	}

	@Override
	public void resizeInterface() {
		//Test carefully in IE8, IE9 and all other browsers if changing this method
		cs.resizeInterface();
		tp.forceLayout();
		super.resizeInterface();		
	}

	@Override
	public String getGuideText() {
		return "Please define at least one sample group to proceed. Start by selecting compounds to the left. Then select doses and times.";
	}
}
