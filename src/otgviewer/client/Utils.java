package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.Group;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

public class Utils {

	private static NumberFormat df = NumberFormat.getDecimalFormat();
	private static NumberFormat sf = NumberFormat.getScientificFormat();
	
	public static String formatNumber(double v) {		
		if (Math.abs(v) > 0.001) {
			return df.format(v);
		} else {
			return sf.format(v);
		}
	}
	
	public static HorizontalPanel mkHorizontalPanel() {
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(2);
		hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		return hp;
	}

	/**
	 * Colour: for example, MediumAquaMarine or LightSkyBlue
	 * @param color
	 * @return
	 */
	public static Options createChartOptions(String color) {
		Options o = Options.create();
		o.setColors(color);				
		o.set("legend.position", "none");
		o.setLegend(LegendPosition.NONE);
		return o;
	}

	public static DataColumn unpackColumn(String s) {
		String[] spl = s.split("^^^");
		if (spl[0].equals("Barcode")) {
			return Barcode.unpack(s);
		} else {
			return Group.unpack(s);
		}
	}

	public static void displayInPopup(Widget w) {
		final PopupPanel pp = new PopupPanel(true, true);					
		pp.setWidget(w);		
		pp.setPopupPositionAndShow(new PositionCallback() {
			public void setPosition(int w, int h) {
				if (h > Window.getClientHeight() - 100) {			
					pp.setHeight((Window.getClientHeight() - 100) + "px");	
					pp.setWidget(makeScrolled(pp.getWidget()));
					pp.setPopupPosition(Window.getClientWidth() - w - 50, 50);
				} else {
					pp.setPopupPosition(Window.getClientWidth() - w - 50, Window.getClientHeight()/2 - h/2);							
				}
			}
		});		
	}
	
	public static Widget makeScrolled(Widget w) {
		ScrollPanel sp = new ScrollPanel(w);		
		return sp;
	}
	
	public static String[] allCompounds(List<DataColumn> columns) {
		List<String> r = new ArrayList<String>();
		for (DataColumn dc: columns) {
			for (String c: dc.getCompounds()) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}
	
}
