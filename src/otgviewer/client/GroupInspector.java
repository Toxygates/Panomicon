package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This widget is intended to help visually define and modify "groups"
 * of microarrays.
 * @author johan
 *
 */
public class GroupInspector extends DataListenerWidget {
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private Grid grid = new Grid();
	private String[] availableTimes;
	private CheckBox[][] checkboxes;
	private Map<String, Group> groups = new HashMap<String, Group>();
	private TextBox txtbxGroup;
	
	public GroupInspector() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		
		Label lblGroupDefinition = new Label("Group definition");
		lblGroupDefinition.setStyleName("heading");
		vp.add(lblGroupDefinition);
		
		HorizontalPanel horizontalPanel_1 = new HorizontalPanel();
		vp.add(horizontalPanel_1);
		
		Button btnSelectAll = new Button("Select all");
		horizontalPanel_1.add(btnSelectAll);
		btnSelectAll.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				drawGridInner(true);
			}
		});
		
		Button btnSelectNone = new Button("Select none");
		horizontalPanel_1.add(btnSelectNone);
		btnSelectNone.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				drawGridInner(false);
			}
		});
		
		grid.setStyleName("highlySpaced");
		grid.setWidth("700px");
		grid.setHeight("400px");
		grid.setBorderWidth(0);
		vp.add(grid);
		vp.setWidth("410px");		
		
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		vp.add(horizontalPanel);
		
		Label lblSaveGroupAs = new Label("Save group as");
		horizontalPanel.add(lblSaveGroupAs);
		
		
		txtbxGroup = new TextBox();
		txtbxGroup.setText("Group 1");
		horizontalPanel.add(txtbxGroup);
		
		Button btnSave = new Button("Save");
		horizontalPanel.add(btnSave);
		btnSave.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				makeGroup(txtbxGroup.getValue());
			}
		});
		
	}
	
	private void lazyFetchTimes() {
		if (availableTimes != null) {
			drawGridInner(false);
		} else if (chosenCompounds.size() > 0) {
			owlimService.times(chosenDataFilter, chosenCompounds.get(0), chosenDataFilter.organ.toString(), new AsyncCallback<String[]>() {
				public void onSuccess(String[] times) {
					availableTimes = times;
					drawGridInner(false);
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to get sample times.");
				}
			});			
		}		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		chosenDataFilter = filter;
		availableTimes = null;
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		chosenCompounds = compounds;
		redrawGrid();
	}
	
	private void redrawGrid() {
		grid.resize(chosenCompounds.size() + 1, 4);
		
		for (int i = 1; i < chosenCompounds.size() + 1; ++i) {
			grid.setWidget(i, 0, new Label(chosenCompounds.get(i - 1)));
		}
		
		grid.setWidget(0, 1, new Label("Low"));
		grid.setWidget(0, 2, new Label("Medium"));
		grid.setWidget(0, 3, new Label("High"));
		
		grid.setHeight(50 * (chosenCompounds.size() + 1) + "px");
		lazyFetchTimes();
	}
	
	private void drawGridInner(boolean initState) {
		checkboxes = new CheckBox[chosenCompounds.size()][];
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			checkboxes[c] = new CheckBox[3 * availableTimes.length];
			for (int d = 0; d < 3; ++d) {
				HorizontalPanel hp = new HorizontalPanel();
				for (int t = 0; t < availableTimes.length; ++t) {				
					CheckBox cb = new CheckBox(availableTimes[t]);
					cb.setValue(initState);					
					checkboxes[c][availableTimes.length * d + t] = cb;
					hp.add(cb);					
				}
				CheckBox all = new CheckBox("All");
				all.addValueChangeHandler(new MultiSelectHandler(c, availableTimes.length * d, availableTimes.length * (d + 1)));
				hp.add(all);
				
				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("border");					
				grid.setWidget(c + 1, d + 1, sp);					
			}
		}
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	Group pendingGroup;
	private void makeGroup(String name) {
		List<Barcode> barcodes = new ArrayList<Barcode>();
		if (groups.containsKey(name)) {
			Window.alert("A group with that name has already been defined.");
			return;
		}

		pendingGroup = new Group(name, new Barcode[0]);

		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (checkboxes[c][availableTimes.length * d + t].getValue()) {
						String compound = chosenCompounds.get(c);
						String dose = "";
						switch (d) {
						case 0:
							dose = "Low";
							break;
						case 1:
							dose = "Middle";
							break;
						case 2:
							dose = "High";
							break;
						}
						String time = availableTimes[t];

						owlimService.barcodes(chosenDataFilter, compound,
								chosenDataFilter.organ.toString(), dose, time,
								new AsyncCallback<Barcode[]>() {
									public void onSuccess(Barcode[] barcodes) {
										addToGroup(barcodes);
									}

									public void onFailure(Throwable caught) {
										Window.alert("Unable to retrieve barcodes for the group definition.");
									}
								});
					}
				}
			}
		}
	}
	
	private void addToGroup(Barcode[] barcodes) {		
		synchronized (this) {			
			List<Barcode> n = new ArrayList<Barcode>();
			n.addAll(Arrays.asList(barcodes));			
			n.addAll(Arrays.asList(pendingGroup.getBarcodes()));
			pendingGroup = new Group(pendingGroup.getName(),
					n.toArray(new Barcode[0]));
			groups.put(pendingGroup.getName(), pendingGroup);
		}
	
	}
	
	private class MultiSelectHandler implements ValueChangeHandler<Boolean> {
		private int from, to, row;
		MultiSelectHandler(int row, int from, int to) {
			this.from = from;
			this.to = to;
			this.row = row;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (int i = from; i < to; ++i) {
				checkboxes[row][i].setValue(vce.getValue());
			}
		}
	}
}
