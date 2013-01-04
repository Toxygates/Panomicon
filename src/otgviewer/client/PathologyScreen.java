package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Barcode;
import otgviewer.shared.CellType;
import otgviewer.shared.DataColumn;
import otgviewer.shared.Group;
import otgviewer.shared.Pathology;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PathologyScreen extends Screen {
	public static final String key = "pc";

	private CellTable<Pathology> pathologyTable = new CellTable<Pathology>();
	private ScrollPanel sp = new ScrollPanel();
	private VerticalPanel vp = new VerticalPanel();
	private List<Pathology> pathologies = new ArrayList<Pathology>();
	final Screen myScreen = this;
	private static Resources resources = GWT.create(Resources.class);
	
	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key) && chosenDataFilter.cellType == CellType.Vivo;
	}

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	
	public PathologyScreen(ScreenManager man) {
		super("Pathologies", key, true, true, man);
	}

	public Widget content() {		
		vp.add(sp);
		sp.setWidget(pathologyTable);
		
		TextColumn<Pathology> col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				Group g = Utils.groupFor(chosenColumns, p.barcode());
				if (g != null) {
					return g.getName();
				} else {
					return "None";
				}							
			}
		}; 
		pathologyTable.addColumn(col, "Group");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				Barcode b = Utils.barcodeFor(chosenColumns, p.barcode());
				return b.getCompound() + "/" + b.getShortTitle(); 				
			}
		};
		pathologyTable.addColumn(col, "Sample");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.finding();
			}
		};
		
		ToolColumn tcl = new ToolColumn(new InspectCell());
		pathologyTable.addColumn(tcl, "");
		pathologyTable.setColumnWidth(tcl, "40px");		
		
		pathologyTable.addColumn(col, "Finding");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.topography();
			}
		};
		pathologyTable.addColumn(col, "Topography");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.grade();
			}
		};
		pathologyTable.addColumn(col, "Grade");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return "" + p.spontaneous();
			}
		};
		pathologyTable.addColumn(col, "Spontaneous");		
		
		return vp;
	}
	
	
	@Override
	public void show() {
		super.show();
		if (visible) {
			columnsChanged(chosenColumns);
		}
	}

	
	@Override
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		if (visible) {
			pathologies.clear();
			for (DataColumn c : columns) {
				owlimService.pathologies(c, new AsyncCallback<Pathology[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to get pathologies.");
					}

					public void onSuccess(Pathology[] values) {
						pathologies.addAll(Arrays.asList(values));
						pathologyTable.setRowData(pathologies);
					}
				});
			}
		}
	}
	
	int lastHeight = 0;
	@Override
	public void heightChanged(int newHeight) {
		if (newHeight != lastHeight) {
			lastHeight = newHeight;
			super.heightChanged(newHeight);
			vp.setHeight((newHeight - vp.getAbsoluteTop() - 50) + "px");
			sp.setHeight((newHeight - sp.getAbsoluteTop() - 70) + "px");
		}
	}
	
	class InspectCell extends ImageClickCell {
		InspectCell() {
			super(resources.magnify());
		}
		
		public void onClick(String value) {
			storeCustomColumn(Utils.barcodeFor(chosenColumns, value));
			configuredProceed(SampleDetailScreen.key);
		}
	}
	
	class ToolColumn extends Column<Pathology, String> {		
		public ToolColumn(InspectCell tc) {
			super(tc);			
		}
		
		public String getValue(Pathology p) {
			return p.barcode();			
		}
	}
}
