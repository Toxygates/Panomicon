package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
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

	private SeriesDisplayStrategy seriesStrategy;
	private Label seriesSelectionLabel;
	private ListBox chartCombo, chartSubtypeCombo;
	private DataTable seriesTable;
	private CoreChart seriesChart;
	private DockPanel chartDockPanel;
	private boolean isSlave;

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);

	private List<SeriesChart> slaveCharts = new ArrayList<SeriesChart>();
	
	void addSlaveChart(SeriesChart slave) {
		slaveCharts.add(slave);
	}
	
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

	public SeriesChart(boolean isSlave) {
		
		VisualizationUtils
		.loadVisualizationApi("1.1", new Runnable() {
			public void run() {
				seriesTable = DataTable.create();
			}
		}, "corechart");

		this.isSlave = isSlave;
		
		chartDockPanel = new DockPanel();
		chartDockPanel.setSize("100%", "100%");
		initWidget(chartDockPanel);

		VerticalPanel verticalPanel = new VerticalPanel();
		chartDockPanel.add(verticalPanel, DockPanel.NORTH);
		verticalPanel.setWidth("500px");

		seriesSelectionLabel = new Label("Selected: none");
		verticalPanel.add(seriesSelectionLabel);

		if (!isSlave) {
			HorizontalPanel horizontalPanel_2 = new HorizontalPanel();
			horizontalPanel_2
			.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
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

	}

	void redraw() {
		if (!isSlave) {
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

				// first find the applicable barcodes
				if (chartCombo.getSelectedIndex() == 0) {
					redrawForDose(chartSubtypeCombo
							.getItemText(chartSubtypeCombo.getSelectedIndex()));
				} else {
					redrawForTime(chartSubtypeCombo
							.getItemText(chartSubtypeCombo.getSelectedIndex()));

				}
			}
		}
	}
	
	void redrawForTime(String time) {
		seriesStrategy = new SeriesDisplayStrategy.VsDose(seriesTable);
		owlimService.barcodes(chosenDataFilter, chosenCompound,
				null, time,
				seriesChartBarcodesCallback);
		for (SeriesChart sc: slaveCharts) {
			sc.redrawForTime(time);
		}
	}
	
	void redrawForDose(String dose) {
		seriesStrategy = new SeriesDisplayStrategy.VsTime(seriesTable);
		owlimService.barcodes(chosenDataFilter, chosenCompound,
				dose, null,
				seriesChartBarcodesCallback);
		for (SeriesChart sc: slaveCharts) {
			sc.redrawForDose(dose);
		}
	}

	void updateSeriesSubtypes() {
		chartSubtypeCombo.clear();
		if (chartCombo.getSelectedIndex() == 0) {
			getDosesForSeriesChart();
		} else {			
			getTimesForSeriesChart();
		}
	}

	void getDosesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.doseLevels(chosenDataFilter, chosenCompound, seriesChartItemsCallback);
	}

	void getTimesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.times(chosenDataFilter, chosenCompound, seriesChartItemsCallback);
	}

	private void updateSelectionLabel() {

		switch (chosenDataFilter.cellType) {
		case Vivo:
			seriesSelectionLabel.setText("Selected: "					
					+ chosenCompound + "/"					
					+ chosenDataFilter.repeatType + "/" + chosenValueType
					+ "/" + chosenProbe);
			break;
		case Vitro:
			seriesSelectionLabel.setText("Selected: "
					+ chosenCompound + "/"
					+ chosenValueType + "/" + chosenProbe);
			break;
		}		
	}
}
