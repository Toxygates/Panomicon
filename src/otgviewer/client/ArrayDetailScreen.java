package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Annotation;
import otgviewer.shared.Barcode;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ArrayDetailScreen extends Screen {

	public static final String key = "ad";
	
	private CellTable<Annotation.Entry> annotationTable = new CellTable<Annotation.Entry>();
	private ScrollPanel sp = new ScrollPanel();
	private VerticalPanel vp = new VerticalPanel();
	private List<Annotation.Entry> annotations = new ArrayList<Annotation.Entry>();
	private Barcode barcode;
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	
	public ArrayDetailScreen(Barcode barcode, Screen parent, ScreenManager man) {
		super(parent, "Array " + barcode.getCode() + " details", key, false, man);
		this.barcode = barcode;
	}
	
	public Widget content() {		
		vp.add(sp);
		sp.setWidget(annotationTable);
		
		TextColumn<Annotation.Entry> col = new TextColumn<Annotation.Entry>() {
			public String getValue(Annotation.Entry x) {
				return x.description;											
			}
		}; 
		annotationTable.addColumn(col, "Annotation");
		
		col = new TextColumn<Annotation.Entry>() {
			public String getValue(Annotation.Entry x) {
				return x.value;
			}
		};
		annotationTable.addColumn(col, "Value");
			
		return vp;
	}
	
	@Override
	public void show() {
		super.show();
		owlimService.annotations(barcode, new AsyncCallback<Annotation>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get array annotations.");
			}
			public void onSuccess(Annotation a) {
				annotations.clear();
				annotations.addAll(a.getEntries());
				annotationTable.setRowData(annotations);
			}
		});
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

}
