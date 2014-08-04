package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataFilterEditor;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen allows for column (group) definition as well as compound ranking.
 */
public class ColumnScreen extends Screen {
	public static String key = "columns";
	
	private GroupInspector gi;
	private CompoundSelector cs;
	private HorizontalPanel filterTools;
	private TabLayoutPanel tp;
	private final String rankingLabel;
	
	public ColumnScreen(ScreenManager man, String rankingLabel) {
		super("Sample group definitions", key, false, man,
				resources.groupDefinitionHTML(), resources.groupDefinitionHelp());
		
		this.rankingLabel = rankingLabel;
		
		String majorParam = man.schema().majorParameter();
		cs = new CompoundSelector(this, man.schema().title(majorParam));
		filterTools = mkFilterTools();
		this.addListener(cs);
		cs.setStyleName("compoundSelector");
	} 
	
	private HorizontalPanel mkFilterTools() {
		final Screen s = this;
		HorizontalPanel r = new HorizontalPanel();
		DataFilterEditor dfe = new DataFilterEditor() {
			@Override
			protected void changeSampleClass(SampleClass sc) {
				super.changeSampleClass(sc);				
//				s.dataFilterChanged(sc.asDataFilter());
				s.sampleClassChanged(sc);
//				s.storeDataFilter(s.getParser());
				//TODO I'm not sure that exposing the action queue mechanism 
				//like this is a good thing to do. Think of a better way.
				runActions();
			}
		};
		this.addListener(dfe);
		r.add(dfe);		
		return r;
	}
	
	@Override
	protected void addToolbars() {
		super.addToolbars();
		addToolbar(filterTools, 45);
		addLeftbar(cs, 350);
	}
	
	@Override
	protected boolean shouldShowStatusBar() {
		return false;
	}

	public Widget content() {				
		tp = new TabLayoutPanel(30, Unit.PX);
		
		gi = new GroupInspector(cs, this);
		this.addListener(gi);
		cs.addListener(gi);
		tp.add(gi, "Sample groups");
		
		final CompoundRanker cr = new CompoundRanker(this, cs);
		tp.add(Utils.makeScrolled(cr), rankingLabel);
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
	public void loadState(StorageParser p, DataSchema schema) {
		super.loadState(p, schema);
		if (visible) {
			//If we became visible, we must have been enabled, so can count on a
			//data filter being present.
			try {
				List<Group> ics = loadColumns(p, schema(), "inactiveColumns", 
						new ArrayList<OTGColumn>(gi.existingGroupsTable.inverseSelection()));
				if (ics != null) {
					gi.inactiveColumnsChanged(ics);
				}

			} catch (Exception e) {
				Window.alert("Unable to load inactive columns.");
			}
		}
	}
	
	@Override
	public void changeSampleClass(SampleClass sc) {
		//On this screen, ignore the blank sample class set by
		//DataListenerWidget
		if (!sc.getMap().isEmpty()) {
			super.changeSampleClass(sc);
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
	
	public void displayCompoundRankUI() {
		tp.selectTab(1);
	}
}
