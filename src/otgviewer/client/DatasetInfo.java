package otgviewer.client;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
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
	private static Resources resources = GWT.create(Resources.class);
	
	public DatasetInfo(DataFilter filter, SelectionListener listener, boolean images) {
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
		
		CellType ct = CellType.valueOf(filter.cellType);
		if (ct == CellType.Vivo) {
			description += ", " + filter.organ.toString() + ", " + filter.repeatType.toString() + " dose";			
		}

		if (images) {
			HorizontalPanel icons = new HorizontalPanel();
			icons.setStyleName("darkColored");
			icons.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

			icons.add(new Image(image(Organism.valueOf(filter.organism))));

			if (ct == CellType.Vivo) {
				icons.add(new Image(image(Organ.valueOf(filter.organ))));
			} else {
				icons.add(new Image(resources.vitro()));
			}

			icons.add(new Image(image(RepeatType.valueOf(filter.repeatType))));
			icons.add(new Image(resources.bottle()));

			vp.add(icons);
			icons.setSpacing(5);
		}
		
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
	
	private ImageResource image(Organ organ) {
		switch (organ) {
		case Liver:
			return resources.liver();
		case Lung:
			return resources.lung();
		case Muscle:
			return resources.muscle();
		case Spleen:
			return resources.spleen();
		case Kidney:
			return resources.kidney();
		case LymphNode:
			return null; //TODO
		}
		return null;
	}
	
	private ImageResource image(Organism organism) {
		switch (organism) {
		case Human:
			return resources.human();
		case Rat:
			return resources.rat();
		case Mouse:
			return resources.mouse();
		}
		return null;
	}
	
	private ImageResource image(RepeatType repeat) {
		switch (repeat) {
		case Single:
			return resources.clock();
		case Repeat:
			return resources.calendar();
		}
		return null;
	}
}
