package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
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
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;

public class SeriesChart extends DataListenerWidget {

	public static class Controller extends DataListenerWidget {
		private ListBox chartCombo, chartSubtypeCombo;

		private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
				.create(OwlimService.class);
		
		private List<SeriesChart> charts = new ArrayList<SeriesChart>();
		
		public Controller() {
			HorizontalPanel hp = Utils.mkHorizontalPanel();
			initWidget(hp);
			
			chartCombo = new ListBox();
			hp.add(chartCombo);
			chartCombo.addItem("Expression vs time, fixed dose:");
			chartCombo.addItem("Expression vs dose, fixed time:");
			chartCombo.setSelectedIndex(0);

			chartSubtypeCombo = new ListBox();
			hp.add(chartSubtypeCombo);

			chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
				public void onChange(ChangeEvent event) {				
					redraw();
				}

			});
			chartCombo.addChangeHandler(new ChangeHandler() {
				public void onChange(ChangeEvent event) {
					updateSeriesSubtypes();
				}
			});
		}
		
		public void addChart(SeriesChart sc) { charts.add(sc); }
		

		private AsyncCallback<String[]> seriesChartItemsCallback = new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get series chart subitems.");
			}

			public void onSuccess(String[] result) {
				for (String i : result) {
					if (!i.equals("Control")) {
						chartSubtypeCombo.addItem(i);
					}
				}
				if (result.length > 0) {
					chartSubtypeCombo.setSelectedIndex(0);
					redraw();
				}
			}
		};
		
		public void redraw() {

			// make sure something is selected
			if (chartCombo.getSelectedIndex() == -1) {
				chartCombo.setSelectedIndex(0);
			}
			if (chartSubtypeCombo.getItemCount() == 0) {
				updateSeriesSubtypes(); // will redraw for us later
			} else {

				if (chartSubtypeCombo.getSelectedIndex() == -1) {
					chartSubtypeCombo.setSelectedIndex(0);
				}

				for (SeriesChart sc : charts) {
					// first find the applicable barcodes
					if (chartCombo.getSelectedIndex() == 0) {
						sc.redrawForDose(chartSubtypeCombo
								.getItemText(chartSubtypeCombo
										.getSelectedIndex()));
					} else {
						sc.redrawForTime(chartSubtypeCombo
								.getItemText(chartSubtypeCombo
										.getSelectedIndex()));

					}
				}
			}
		}
		
		private void updateSeriesSubtypes() {
			chartSubtypeCombo.clear();
			if (chartCombo.getSelectedIndex() == 0) {
				getDosesForSeriesChart();
			} else {			
				getTimesForSeriesChart();
			}
		}

		private void getDosesForSeriesChart() {
			chartSubtypeCombo.clear();
			owlimService.doseLevels(chosenDataFilter, chosenCompound, seriesChartItemsCallback);
		}

		private void getTimesForSeriesChart() {
			chartSubtypeCombo.clear();
			owlimService.times(chosenDataFilter, chosenCompound, seriesChartItemsCallback);
		}

	}
	
	private SeriesDisplayStrategy seriesStrategy;
	private Label seriesSelectionLabel;
	
	private DataTable seriesTable;
	private CoreChart seriesChart;
	private DockPanel chartDockPanel;
	private String chosenProbe;
	private Screen screen;

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

	public void changeProbe(String probe) {
		chosenProbe = probe;		
		updateSelectionLabel();
	}
	
	int pixelHeight = 300;
	public void setPixelHeight(int px) {
		setHeight(px + "px");
		pixelHeight = px;
	}

	private AsyncCallback<Barcode[]> seriesChartBarcodesCallback = new AsyncCallback<Barcode[]>() {
		public void onFailure(Throwable caught) {
			Window.alert("Unable to get series chart data (barcodes).");
		}

		public void onSuccess(Barcode[] barcodes) {
			seriesStrategy.setupTable(barcodes);

			List<String> bcs = new ArrayList<String>();
			for (Barcode b : barcodes) {
				bcs.add(b.getCode());
			}
			if (chosenProbe == null) {
				Window.alert("Unable to draw chart. Please select a probe first.");
			} else {
				String[] probes = new String[] { chosenProbe };

				kcService.getFullData(chosenDataFilter, bcs, probes,
						chosenValueType, true,
						new AsyncCallback<List<ExpressionRow>>() {
							public void onSuccess(List<ExpressionRow> result) {
								if (seriesChart != null) {
									chartDockPanel.remove(seriesChart);
								}
								seriesChart = seriesStrategy.makeChart();
								chartDockPanel.add(seriesChart,
										DockPanel.CENTER);
								seriesChart.setHeight((pixelHeight - 50) + "px");
								seriesStrategy.displayData(result, seriesChart); 
							}

							public void onFailure(Throwable caught) {
								Window.alert("Unable to get series chart data (expressions).");
							}
						});
			}
		}
	};

	public SeriesChart(Screen _screen) {
		
		VisualizationUtils
		.loadVisualizationApi("1.1", new Runnable() {
			public void run() {
				seriesTable = DataTable.create();
			}
		}, "corechart");

		screen = _screen;
		
		chartDockPanel = new DockPanel();
		chartDockPanel.setSize("100%", "100%");
		initWidget(chartDockPanel);

		VerticalPanel verticalPanel = new VerticalPanel();
		chartDockPanel.add(verticalPanel, DockPanel.NORTH);
		verticalPanel.setWidth("500px");
		
		seriesSelectionLabel = Utils.mkEmphLabel("Selected: none");
		verticalPanel.add(seriesSelectionLabel);
	}

	
	void redrawForTime(String time) {
		seriesStrategy = new SeriesDisplayStrategy.VsDose(screen, seriesTable);
		owlimService.barcodes(chosenDataFilter, chosenCompound,
				null, time,
				seriesChartBarcodesCallback);
	}
	
	void redrawForDose(String dose) {
		seriesStrategy = new SeriesDisplayStrategy.VsTime(screen, seriesTable);
		owlimService.barcodes(chosenDataFilter, chosenCompound,
				dose, null,
				seriesChartBarcodesCallback);
	}
	
	private void updateSelectionLabel() {		
		seriesSelectionLabel.setText(chosenCompound);		 			
	}
}
