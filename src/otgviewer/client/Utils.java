package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.Group;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
		hp.setStyleName("slightlySpaced");
		hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		return hp;
	}

	public static VerticalPanel mkVerticalPanel() {
		VerticalPanel vp = new VerticalPanel();
		vp.setStyleName("slightlySpaced");
		vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		return vp;
	}

	public static Label mkEmphLabel(String s) {
		Label r = new Label(s);
		r.setStyleName("emphasized");
		return r;
	}

	/**
	 * Colour: for example, MediumAquaMarine or LightSkyBlue
	 * 
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

	public static void displayInPopup(Widget w) {
		final PopupPanel pp = new PopupPanel(true, true);
		pp.setWidget(w);
		pp.setPopupPositionAndShow(displayInCenter(pp));		
	}

	public static PositionCallback displayInCenter(final PopupPanel pp) {
		return new PositionCallback() {			
		public void setPosition(int w, int h) {
			if (h > Window.getClientHeight() - 100) {			
				pp.setHeight((Window.getClientHeight() - 100) + "px");	
				pp.setWidget(makeScrolled(pp.getWidget()));
				pp.setPopupPosition(Window.getClientWidth() - w - 50, 50);
			} else {
				pp.setPopupPosition(Window.getClientWidth() - w - 50, 
						Window.getClientHeight()/2 - h/2);							
			}
		}
		};
	}

	public static Widget makeScrolled(Widget w) {
		ScrollPanel sp = new ScrollPanel(w);
		return sp;
	}
	
	public static String[] allCompounds(List<DataColumn> columns) {
		List<String> r = new ArrayList<String>();
		for (DataColumn dc : columns) {
			for (String c : dc.getCompounds()) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}

	public static Group findGroup(List<DataColumn> groups, String title) {
		for (DataColumn d : groups) {
			if (((Group) d).getName().equals(title)) {
				return ((Group) d);
			}
		}
		return null;
	}

	public static Group groupFor(List<DataColumn> columns, String barcode) {
		for (DataColumn c : columns) {
			for (Barcode b : c.getBarcodes()) {
				if (b.getCode().equals(barcode)) {
					if (c instanceof Group) {
						return (Group) c;
					}
				}
			}
		}
		return null;
	}

	public static Barcode barcodeFor(List<DataColumn> columns, String barcode) {
		for (DataColumn c : columns) {
			for (Barcode b : c.getBarcodes()) {
				if (b.getCode().equals(barcode)) {
					return b;
				}
			}
		}
		return null;
	}

}
