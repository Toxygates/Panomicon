package otgviewer.client;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.Group;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
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

	public static Options createChartOptions() {
		Options o = Options.create();
		o.setColors("MediumAquaMarine");				
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
		PopupPanel pp = new PopupPanel(true, true);
		pp.setWidget(w);
		pp.setPopupPosition(Window.getClientWidth()/2 - 100, Window.getClientHeight()/2 - 100);
		pp.show();
	}
	
	public static void displayInScrolledPopup(Widget w) {
		PopupPanel pp = new PopupPanel(true, true);		
		ScrollPanel sp = new ScrollPanel(w);		
		pp.setWidget(sp);
		sp.setHeight((Window.getClientHeight() - 100) + "px");			
		sp.setWidget(w);		
		pp.setPopupPosition(Window.getClientWidth()/2 - 100, 50);			
		pp.show();	
	}
	
}
