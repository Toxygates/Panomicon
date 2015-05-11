package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import otgviewer.client.components.DataFilterEditor;
import otgviewer.client.components.DatasetSelector;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.SimpleCompoundRanker;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import t.common.shared.DataSchema;
import t.common.shared.Dataset;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.SparqlService;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
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
	private DataFilterEditor dfe;
	
	private SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	
	public ColumnScreen(ScreenManager man, String rankingLabel) {
		super("Sample group definitions", key, false, man,
				resources.groupDefinitionHTML(), resources.groupDefinitionHelp());
		
		this.rankingLabel = rankingLabel;
		
		String majorParam = man.schema().majorParameter();
		cs = new CompoundSelector(this, man.schema().title(majorParam));		
		this.addListener(cs);
		cs.setStylePrimaryName("compoundSelector");
		filterTools = mkFilterTools();
		
		chosenDatasets = appInfo().datasets();		
	} 
	
	private HorizontalPanel mkFilterTools() {
		final Screen s = this;
		HorizontalPanel r = new HorizontalPanel();
		r.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		Button b = new Button("Data...");
		r.add(b);
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				showDatasetSelector();										
			}
		});
		
		dfe = new DataFilterEditor(schema()) {
			@Override
			protected void changeSampleClass(SampleClass sc) {
				super.changeSampleClass(sc);				
				s.sampleClassChanged(sc);
				//TODO I'm not sure that exposing the action queue mechanism 
				//like this is a good thing to do. Think of a better way.
				runActions();
			}
		};
		this.addListener(dfe);
		r.add(dfe);		
		return r;
	}
	
	protected void showDatasetSelector() {
		final DialogBox db = new DialogBox(false, true);
		final Screen scr = this;
		//TODO set init. selection
		DatasetSelector dsel = new DatasetSelector(Arrays.asList(appInfo().datasets()), 
				Arrays.asList(chosenDatasets)) {
			@Override
			public void onOK() {							
				datasetsChanged(getSelection().toArray(new Dataset[0]));
				sparqlService.chooseDatasets(chosenDatasets,
						new PendingAsyncCallback<Void>(scr, "Unable to choose datasets") {					
					public void handleSuccess(Void v) {
						dfe.update();
					}
				});				
				db.hide();				
			}
			
			@Override
			public void onCancel() {
				super.onCancel();
				db.hide();
			}
		};
		db.setText("Select datasets");
		db.setWidget(dsel);
		db.setWidth("500px");
		db.show();
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
		gi.datasetsChanged(chosenDatasets);
		
		tp.add(gi, "Sample groups");
		
		final CompoundRanker cr = new SimpleCompoundRanker(this, cs);
		tp.add(Utils.makeScrolled(cr), rankingLabel);
		tp.selectTab(0);		
		
		gi.addStaticGroups(appInfo().predefinedSampleGroups());		
		return tp;
	}

	@Override
	public Widget bottomContent() {
		HorizontalPanel hp = Utils.mkWidePanel();
		
		Button b = new Button("Delete all groups", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				gi.confirmDeleteAllGroups();				
			}			
		});		
		
		Button b2 = new Button("Next: Select probes", new ClickHandler() {			
			public void onClick(ClickEvent event) {
				if (gi.chosenColumns().size() == 0) {
					Window.alert("Please define and activate at least one group.");
				} else {
					configuredProceed(ProbeScreen.key);					
				}
			}
		});
		
		hp.add(Utils.mkHorizontalPanel(true, b, b2));
		return hp;
	}
	
	@Override
	public void loadState(StorageParser p, DataSchema schema) {
		super.loadState(p, schema);
		if (visible) {
			try {
				List<Group> ics = loadColumns(p, schema(), "inactiveColumns", 
						new ArrayList<OTGColumn>(gi.existingGroupsTable.inverseSelection()));
				if (ics != null && ics.size() > 0) {
					logger.info("Unpacked i. columns: " + ics.get(0) + ": " + ics.get(0).getSamples()[0] + " ... ");
					gi.inactiveColumnsChanged(ics);
				} else {
					logger.info("No i. columns available");
				}

			} catch (Exception e) {
				logger.log(Level.WARNING, "Unable to load i. columns", e);
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
