package otgviewer.client;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This widget is displayed on the first screen, the "dataset selection screen".
 * It displays images and some basic information about each dataset.
 * When it is clicked it fires an event.
 * @author johan
 *
 */
public class DatasetInfo extends Composite implements ClickHandler {
	public static interface SelectionListener {
		void filterSelected(DataFilter filter);
	}
	
	private DataFilter _filter;
	private SelectionListener _listener;
	private Resources resources = GWT.create(Resources.class);
	
	public DatasetInfo(DataFilter filter, SelectionListener listener) {
		_filter = filter;
		_listener = listener;
		
		FocusPanel fp = new FocusPanel();
		initWidget(fp);
		fp.addClickHandler(this);		
		
		VerticalPanel vp = Utils.mkVerticalPanel();
		fp.add(vp);
		vp.setStyleName("datasetInfo");
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
			if (filter.organ == Organ.Liver) {
				icons.add(new Image(resources.liver()));
			} else {
				icons.add(new Image(resources.kidney()));
			}
		} else {
			icons.add(new Image(resources.vitro()));
		}
		
		if (filter.repeatType == RepeatType.Repeat) {
			icons.add(new Image(resources.calendar()));					
		} else {
			icons.add(new Image(resources.clock()));			
		}
		icons.add(new Image(resources.bottle()));
		
		vp.add(icons);		
		icons.setSpacing(5);
		
		VerticalPanel ivp = new VerticalPanel(); //for left alignment
		ivp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		vp.add(ivp);
		Label l = new Label(description);
		l.setStyleName("heading");
		ivp.add(l);
		ivp.setWidth("100%");

		
		ivp = new VerticalPanel();
		ivp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		ivp.setWidth("100%");
		vp.add(ivp);			
	}
	
	public void onClick(ClickEvent ce) {
		_listener.filterSelected(_filter);
	}
	
}
