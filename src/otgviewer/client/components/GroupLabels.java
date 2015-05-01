package otgviewer.client.components;

import java.util.List;

import otgviewer.client.Utils;
import otgviewer.shared.Group;
import t.viewer.shared.DataSchema;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class GroupLabels extends Composite {

	private List<Group> groups;
	private DataSchema schema;
	private FlowPanel fpo;
	private Screen screen;
	
	public GroupLabels(Screen screen, DataSchema schema, List<Group> groups) {
		fpo = new FlowPanel();
		this.groups = groups;
		this.schema = schema;
		this.screen = screen;
		initWidget(fpo);
		showSmall();
	}
	
	private void show(List<Group> groups) {
		fpo.clear();
		for (Group g: groups) {			
			FlowPanel fp = new FlowPanel();
			fp.setStylePrimaryName("statusBorder");
			String tip = g.getSamples()[0].sampleClass().label(schema) + ":\n" +
					g.getTriples(schema, -1, ", ");
			Label l = Utils.mkEmphLabel(g.getName() + ":");
			l.setWordWrap(false);
			l.getElement().getStyle().setMargin(2, Unit.PX);
			l.setStylePrimaryName(g.getStyleName());
			Utils.floatLeft(fp, l);
			l.setTitle(tip);
			l = new Label(g.getTriples(schema, 2, ", "));
			l.getElement().getStyle().setMargin(2, Unit.PX);
			l.setStylePrimaryName(g.getStyleName());
			Utils.floatLeft(fp, l);
			l.setTitle(tip);
			l.setWordWrap(false);
			Utils.floatLeft(fpo, fp);				
		}		
	}
	
	private void showAll() {
		show(groups);
		if (groups.size() > 5) {
			Button b = new Button("Hide", new ClickHandler() {			
				@Override
				public void onClick(ClickEvent event) {
					showSmall();				
				}				
			});	
			Utils.floatLeft(fpo, b);
		}
		screen.resizeInterface();
	}
	
	private void showSmall() {
		if (groups.size() > 5) {
			List<Group> gs = groups.subList(0, 5);
			show(gs);
			Button b = new Button("Show all", new ClickHandler() {			
				@Override
				public void onClick(ClickEvent event) {
					showAll();				
				}				
			});	
			Utils.floatLeft(fpo, b);
		} else {
			show(groups);		
		}
		screen.resizeInterface();
	}

}
