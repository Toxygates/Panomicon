package otgviewer.client;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class DatasetInfo extends Composite implements ClickHandler {
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	
	public static interface SelectionListener {
		void filterSelected(DataFilter filter);
	}
	
	private DataFilter _filter;
	private SelectionListener _listener;
	private Label compoundsLabel, doseLabel, timeLabel;
	private Resources resources = GWT.create(Resources.class);
	
	public DatasetInfo(DataFilter filter, SelectionListener listener) {
		_filter = filter;
		_listener = listener;
		
		VerticalPanel vp = Utils.mkVerticalPanel();		
		initWidget(vp);
		vp.setStyleName("border");
		vp.setWidth("100%");
		vp.setHeight("100%");
		
		String description = filter.organism.toString();
		description += ", " + filter.cellType.toString();
		
		if (filter.cellType == CellType.Vivo) {
			description += ", " + filter.organ.toString() + ", " + filter.repeatType.toString() + " dose";			
		}
		

		HorizontalPanel icons = new HorizontalPanel();		
		icons.setStyleName("darkColored");		
		icons.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		
		if (filter.organism == Organism.Human) {
			icons.add(new Image(resources.human()));
		} else {
			icons.add(new Image(resources.rat()));
		}

		if (filter.cellType == CellType.Vivo) {
			icons.add(new Image(resources.vivo()));
			if (filter.organ == Organ.Liver) {
				icons.add(new Image(resources.liver()));
			} else {
				icons.add(new Image(resources.kidney()));
			}
		} else {
			icons.add(new Image(resources.vitro()));
		}
		
		icons.add(new Image(resources.bottle()));
		if (filter.repeatType == RepeatType.Repeat) {
			icons.add(new Image(resources.bottle()));					
		}
		
		vp.add(icons);		
		icons.setSpacing(5);
		
		VerticalPanel ivp = new VerticalPanel(); //for left alignment
		ivp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		vp.add(ivp);
		Label l = new Label(description);
		l.setStyleName("heading");
		ivp.add(l);
		ivp.setWidth("100%");
		ivp.setHeight("100px");
		
		
		compoundsLabel = new Label("Retrieving compounds...");
		ivp.add(compoundsLabel);
		
		doseLabel = new Label("Retrieving doses...");
		ivp.add(doseLabel);
		
		timeLabel = new Label("Retrieving times...");
		ivp.add(timeLabel);
		
		owlimService.compounds(filter, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				compoundsLabel.setText("Compounds: " + previewString(result));				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				compoundsLabel.setText("Error");				
			}
		});
		
		owlimService.doseLevels(filter, null, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				doseLabel.setText("Doses: " + previewString(result));				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				doseLabel.setText("Error");				
			}
		});
		
		owlimService.times(filter, null, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				timeLabel.setText("Sample times: " + previewString(result));				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				timeLabel.setText("Error");				
			}
		});
		
		ivp = new VerticalPanel();
		ivp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		ivp.setWidth("100%");
		vp.add(ivp);
		Button b = new Button("Select");
		ivp.add(b);
		b.addClickHandler(this);		
	}
	
	public void onClick(ClickEvent ce) {
		_listener.filterSelected(_filter);
	}
	
	private static String previewString(String[] items) {
		String r = "";
		if (items.length > 0) {
			r += items[0];
		}
		if (items.length > 1) {
			r += ", " + items[1];
		}
		if (items.length > 2) {
			r += ", " + items[2];
		}
		if (items.length > 4) {
			r += "... (" + items.length + ")";
		} else if (items.length == 4) {
			r += ", " + items[3];
		}
		return r;
	}
}
