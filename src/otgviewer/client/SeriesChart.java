package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;

public class SeriesChart extends DataListenerWidget {

	private SeriesDisplayStrategy seriesStrategy;
	private Label seriesSelectionLabel;
	private ListBox chartCombo, chartSubtypeCombo;	
	private DataTable seriesTable;	
	private CoreChart seriesChart;
	private DockPanel chartDockPanel;
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	
	@Override
	public void dataFilterChanged(DataFilter df) {
		super.dataFilterChanged(df);		
		updateSelectionLabel();		
	}
	
	@Override
	public void compoundChanged(String compound) {
		super.compoundChanged(compound);		
		updateSelectionLabel();
	}
	
	@Override
	public void valueTypeChanged(ValueType vt) {
		super.valueTypeChanged(vt);
		updateSelectionLabel();
	}
	
	@Override
	public void probeChanged(String probe) {
		super.probeChanged(probe);
		updateSelectionLabel();
	}
	
	private AsyncCallback<String[]> seriesChartItemsCallback = new AsyncCallback<String[]>() {
		public void onFailure(Throwable caught) {
			Window.alert("Unable to get series chart subitems.");
		}
		
		public void onSuccess(String[] result) {
			for (String i: result) {
				if (! i.equals("Control")) {
					chartSubtypeCombo.addItem(i);
				}
			}
			if (result.length > 0) {
				chartSubtypeCombo.setSelectedIndex(0);
				redraw();
			}
		}
	};

	private AsyncCallback<Barcode[]> seriesChartBarcodesCallback = new AsyncCallback<Barcode[]>() {
		public void onFailure(Throwable caught) {
			Window.alert("Unable to get series chart data (barcodes).");
		}
		
		public void onSuccess(Barcode[] barcodes) {
			seriesStrategy.setupTable(barcodes);
			
			List<String> bcs = new ArrayList<String>();
			for (Barcode b: barcodes) {											
				bcs.add(b.getCode());
			}
			if (chosenProbe == null) {
				Window.alert("Unable to draw chart. Please select a probe first.");
			} else {
				String[] probes = new String[] { chosenProbe };

				kcService.getFullData(chosenDataFilter, bcs, probes, chosenValueType, true,
						new AsyncCallback<List<ExpressionRow>>() {
							public void onSuccess(List<ExpressionRow> result) {
								if (seriesChart != null) {
									chartDockPanel.remove(seriesChart);
								}
								seriesChart = seriesStrategy.makeChart();
								chartDockPanel.add(seriesChart,
										DockPanel.CENTER);
								seriesStrategy.displayData(result, seriesChart);
							}

							public void onFailure(Throwable caught) {
								Window.alert("Unable to get series chart data (expressions).");
	}
			});
			}
		}
	};
		
	public SeriesChart() {
				
		chartDockPanel = new DockPanel();			
		chartDockPanel.setSize("100%", "100%");
		initWidget(chartDockPanel);
		
		VerticalPanel verticalPanel = new VerticalPanel();
		chartDockPanel.add(verticalPanel, DockPanel.NORTH);
		verticalPanel.setWidth("276px");
		
		seriesSelectionLabel = new Label("Selected: none");
		verticalPanel.add(seriesSelectionLabel);
		
		HorizontalPanel horizontalPanel_2 = new HorizontalPanel();
		horizontalPanel_2.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		verticalPanel.add(horizontalPanel_2);
		
				chartCombo = new ListBox();
				horizontalPanel_2.add(chartCombo);
				chartCombo.addItem("Expression vs time, fixed dose:");
				chartCombo.addItem("Expression vs dose, fixed time:");
				chartCombo.setSelectedIndex(0);
				
				chartSubtypeCombo = new ListBox();
				horizontalPanel_2.add(chartSubtypeCombo);
				
				chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						seriesTable.removeRows(0, seriesTable.getNumberOfRows());
						redraw();						
					}
					
				});
				chartCombo.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						updateSeriesSubtypes();						
					}
				});
	}
	
	public void onLoadChart() {
		seriesTable = DataTable.create();
	}
	
	@Override
	public void activate() {
		super.activate();
		redraw();
	}
	
	void redraw() {
		if (!active) {
			return;
		}
		//make sure something is selected
		if (chartCombo.getSelectedIndex() == -1) {
			chartCombo.setSelectedIndex(0);
			updateSeriesSubtypes();
		}
		
		if (chartSubtypeCombo.getSelectedIndex() == -1) {
			chartSubtypeCombo.setSelectedIndex(0);
		}
		
		//first find the applicable barcodes
		if (chartCombo.getSelectedIndex() == 0) {
			//select for specific dose.		
			owlimService.barcodes(chosenDataFilter, chosenCompound, chosenDataFilter.organ.toString(), 
					chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex()), null, 
					seriesChartBarcodesCallback);
		} else {
			//select for specific time.
			owlimService.barcodes(chosenDataFilter, chosenCompound, chosenDataFilter.organ.toString(), null, 
					chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex()), 
					seriesChartBarcodesCallback);
		}
	}
	
	void updateSeriesSubtypes() {
		chartSubtypeCombo.clear();
		if (chartCombo.getSelectedIndex() == 0) {					
			seriesStrategy = new SeriesDisplayStrategy.VsTime(seriesTable);
			getDosesForSeriesChart();
		} else {
			seriesStrategy = new SeriesDisplayStrategy.VsDose(seriesTable);
			getTimesForSeriesChart();	
		}
	}

	void getDosesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.doseLevels(chosenDataFilter, chosenCompound, 
				chosenDataFilter.organ.toString(), seriesChartItemsCallback);
	}
	
	void getTimesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.times(chosenDataFilter, chosenCompound, 
				chosenDataFilter.organ.toString(), seriesChartItemsCallback);
	}
	
	private void updateSelectionLabel() {
		switch (chosenDataFilter.cellType) {
		case Vivo:
			seriesSelectionLabel.setText("Selected: " + chosenDataFilter.organism + "/" +
				    chosenDataFilter.organ + "/"  + chosenCompound + "/" + chosenDataFilter.cellType + "/" +  chosenDataFilter.repeatType + "/" + 
					chosenValueType + "/" + chosenProbe);
			break;
		case Vitro:
			seriesSelectionLabel.setText("Selected: " + chosenDataFilter.organism + "/" +
				     chosenCompound + "/" + chosenDataFilter.cellType + "/" + "/" + chosenValueType + "/" + 
					chosenProbe);
			break;
		}		
	}
}
