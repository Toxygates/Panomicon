package otgviewer.client.components.ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.CompoundSelector;
import otgviewer.client.GeneOracle;
import otgviewer.client.Resources;
import otgviewer.client.Utils;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.RankRule;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.ItemList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This widget is an UI for defining compound ranking rules.
 * The actual ranking is requested by a compound selector and performed
 * on the server side.
 */
abstract public class CompoundRanker extends DataListenerWidget {
	protected final Resources resources; 
	final CompoundSelector selector;
	protected final Screen screen;
	protected ListChooser listChooser;
	
	final GeneOracle oracle;
	List<String> availableCompounds = chosenCompounds;
	
	protected final SparqlServiceAsync sparqlService;
	protected final MatrixServiceAsync matrixService;
	
	protected VerticalPanel csVerticalPanel = new VerticalPanel();
	protected List<String> rankProbes = new ArrayList<String>();
	
	protected List<RuleInputHelper> inputHelpers = new ArrayList<RuleInputHelper>();

	protected Grid grid;
	
	final DataSchema schema;
	
	/**
	 * 
	 * @param selector the selector that this CompoundRanker will communicate with.
	 */
	public CompoundRanker(Screen _screen, CompoundSelector selector) {
		this.selector = selector;
		screen = _screen;
		oracle = new GeneOracle(screen);
		schema = screen.schema();
		resources = screen.resources();
		sparqlService = _screen.sparqlService();
		matrixService = _screen.matrixService();
		
		selector.addListener(this);
		listChooser = new ListChooser(screen.appInfo().predefinedProbeLists(),
				"probes") {
			@Override 
			protected void preSaveAction() {
				String[] probes = getProbeList().toArray(new String[0]);
				//We override this to pull in the probes, because they
				//may need to be converted from gene symbols.
				
				matrixService.identifiersToProbes(probes, true, false,  
						new PendingAsyncCallback<String[]>(this) {
					public void handleSuccess(String[] resolved) {
						setItems(Arrays.asList(resolved));
						saveAction(); 
					}					
				});
			}
			
			@Override
			protected void itemsChanged(List<String> items) {
				matrixService.identifiersToProbes(
						items.toArray(new String[0]), true, false,
						new PendingAsyncCallback<String[]>(this) {
					public void handleSuccess(String[] resolved) {
						setProbeList(Arrays.asList(resolved));
					}					
				});
			}
			
			@Override
			protected void listsChanged(List<ItemList> lists) {
				screen.itemListsChanged(lists);
				screen.storeItemLists(getParser(screen));
			}
		};
		listChooser.setStylePrimaryName("colored");
		selector.addListener(listChooser);
		
		csVerticalPanel
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(csVerticalPanel);
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, listChooser);
		csVerticalPanel.add(hp);

		grid = new Grid(1, gridColumns()); //Initially space for 1 rule
		csVerticalPanel.add(grid);
		addHeaderWidgets();
		
		addRule(true);
		
		hp = Utils.mkHorizontalPanel(true);
		csVerticalPanel.add(hp);
		
		hp.add(new Button("Rank", new ClickHandler() {
			public void onClick(ClickEvent event) {
				performRanking();
			}
		}));
		
		
		Widget i = Utils.mkHelpButton(resources.compoundRankingHTML(), resources.compoundRankingHelp());
		hp.add(i);
	}

	protected abstract int gridColumns();
	
	protected abstract void addHeaderWidgets();
	
	protected abstract RuleInputHelper makeInputHelper(RankRule r, boolean isLast);
	
	void addRule(boolean isLast) {
		int ruleIdx = inputHelpers.size();
		grid.resize(ruleIdx + 2, gridColumns());
		RankRule r = new RankRule();		
		RuleInputHelper rih = makeInputHelper(r, isLast);		
		inputHelpers.add(rih);	
		rih.populate(grid, ruleIdx);
	}
	
	/**
	 * Map the current ranking rules to a list of probes and return the result.
	 * @return
	 */
	private List<String> getProbeList() {
		List<String> r = new ArrayList<String>();
		for (RuleInputHelper rih: inputHelpers) {
			if (!rih.probeText.getText().equals("")) {
				String probe = rih.probeText.getText();
				r.add(probe);
			}
		}
		return r;
	} 
	
	/**
	 * Replace the current ranking rules with new rules generated from a list of probes.
	 * @param probes
	 */
	private void setProbeList(List<String> probes) {
		for (RuleInputHelper rih: inputHelpers) {
			rih.reset();			
		}
		for (int i = 0; i < probes.size(); ++i) {
			String probe = probes.get(i);			
			if (i >= inputHelpers.size()) {
				addRule(true);
			}
			inputHelpers.get(i).probeText.setText(probe);
			inputHelpers.get(i).enabled.setValue(true);
		}				
		if (probes.size() == inputHelpers.size()) {
			addRule(true);
		}
	}
	
	private void performRanking() {
		List<RankRule> rules = new ArrayList<RankRule>();
		rankProbes = new ArrayList<String>();		
		for (RuleInputHelper rih: inputHelpers) {
			if (rih.enabled.getValue()) {
				if (rih.probeText.getText().equals("")) {
					Window.alert(
							"Empty gene name detected. Please specify a gene/probe for each enabled rule.");
				} else {
					String probe = rih.probeText.getText();
					rankProbes.add(probe);
					try {
						rules.add(rih.getRule());
					} catch (RankRuleException rre){
						Window.alert(rre.getMessage());
					}
				}		
			}
		}
			
		selector.performRanking(rankProbes, rules);
	}

	
	@Override
	public void sampleClassChanged(SampleClass sc) {
		super.sampleClassChanged(sc);
		oracle.setFilter(sc);	
		for (RuleInputHelper rih: inputHelpers) {
			rih.sampleClassChanged(sc);
		}		
	}

	@Override
	public void availableCompoundsChanged(List<String> compounds) {
		super.availableCompoundsChanged(compounds);
		availableCompounds = compounds;
		for (RuleInputHelper rih: inputHelpers) {
			rih.availableCompoundsChanged(compounds);			
		}
	}
	
	@Override
	public void itemListsChanged(List<ItemList> lists) {
		super.itemListsChanged(lists);
		listChooser.setLists(lists);
	}
}
