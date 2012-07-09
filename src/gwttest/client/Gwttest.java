package gwttest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Gwttest implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT.create(KCService.class);
	
	private ListBox compoundList, organList, doseLevelList, barcodeList;
	private DataGrid<ExpressionRow> exprGrid;
	private ListDataProvider<ExpressionRow> listDataProvider;
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {


		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("850", "");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);
		
		NumberCell nc = new NumberCell();
		
		VerticalPanel verticalPanel = new VerticalPanel();
		rootPanel.add(verticalPanel);
		
		FlowPanel flowPanel = new FlowPanel();
		verticalPanel.add(flowPanel);
		flowPanel.setSize("", "");
		
		compoundList = new ListBox();
		flowPanel.add(compoundList);			
		compoundList.setSize("210px", "202px");
		compoundList.setVisibleItemCount(10);
		compoundList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compounds[compoundList.getSelectedIndex()];
				getOrgans(compound);
				getDoseLevels(compound, null);
			}
		});
		
		
		organList = new ListBox();
		flowPanel.add(organList);
		organList.setSize("11em", "202px");
		organList.setVisibleItemCount(10);
		organList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compounds[compoundList.getSelectedIndex()];
				String organ = organs[organList.getSelectedIndex()];
				getDoseLevels(compound, organ);
			}
		});
		
		doseLevelList = new ListBox();		
		flowPanel.add(doseLevelList);
		doseLevelList.setSize("10em", "202px");
		doseLevelList.setVisibleItemCount(10);
		doseLevelList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String doseLevel = doseLevels[doseLevelList.getSelectedIndex()];
				String organ = organs[organList.getSelectedIndex()];
				String compound = compounds[compoundList.getSelectedIndex()];
				getBarcodes(compound, organ, doseLevel);
			}
		});
		
		barcodeList = new ListBox();
		barcodeList.setVisibleItemCount(10);
		flowPanel.add(barcodeList);
		barcodeList.setSize("15em", "202px");

		exprGrid = new DataGrid<ExpressionRow>();		
		exprGrid.setSize("", "200px");
		exprGrid.setPageSize(20);
		exprGrid.setEmptyTableWidget(new HTML("No Data to Display"));
		
		SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
		SimplePager exprPager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
		exprPager.setDisplay(exprGrid);
		verticalPanel.add(exprPager);

		TextColumn<ExpressionRow> probeCol = new TextColumn<ExpressionRow>() {
			public String getValue(ExpressionRow er) {
				return er.getProbe();
			}
		};
		exprGrid.addColumn(probeCol);
		Column<ExpressionRow, Number> valueCol = new Column<ExpressionRow, Number>(nc) {
			public Double getValue(ExpressionRow er) {
				return er.getValue();
			}
		};
		exprGrid.addColumn(valueCol);
		verticalPanel.add(exprGrid);
		
		listDataProvider = new ListDataProvider<ExpressionRow>();
		listDataProvider.addDataDisplay(exprGrid);
		
		barcodeList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String barcode = barcodes[barcodeList.getSelectedIndex()];
				getExpressions(barcode);				
			}
		});
		
		//get compounds
		getCompounds();
		getOrgans(null);
		getDoseLevels(null, null);		

	}
	
	private String[] compounds = new String[0];
	void getCompounds() {
		compoundList.clear();
		owlimService.compounds(new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get compounds.");				
			}
			public void onSuccess(String[] result) {
				compounds = result;
				for (String compound: result) {					
					compoundList.addItem(compound);					
				}				
			}
		});
	}
	
	private String[] organs = new String[0];
	void getOrgans(String compound) {
		organList.clear();
		owlimService.organs(compound, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get organs.");				
			}
			public void onSuccess(String[] result) {
				organs = result;
				for (String organ: result) {					
					organList.addItem(organ);					
				}				
			}
		});
	}
	
	private String[] doseLevels = new String[0];
	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(null, null, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get dose levels.");				
			}
			public void onSuccess(String[] result) {
				doseLevels = result;				
				for (String doseLevel: result) {					
					doseLevelList.addItem(doseLevel);					
				}				
			}
		});
	}
	
	private String[] barcodes = new String[0];
	void getBarcodes(String compound, String organ, String doseLevel) {
		barcodeList.clear();
		owlimService.barcodes(compound, organ, doseLevel, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get barcodes.");				
			}
			public void onSuccess(String[] result) {
				barcodes = result;
				for (String barcode: result) {							
					barcodeList.addItem(barcode);					
				}				
			}
		});
	}
	
	void getExpressions(String barcode) {
		kcService.absoluteValues(barcode, new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values.");				
			}
			public void onSuccess(List<ExpressionRow> result) {				
				exprGrid.setRowCount(result.size());								
				listDataProvider.setList(result);					
			}
		});
		
	}
}
