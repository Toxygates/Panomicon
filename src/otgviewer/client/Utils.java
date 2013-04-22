package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

public class Utils {

	private static NumberFormat df = NumberFormat.getDecimalFormat();
	private static NumberFormat sf = NumberFormat.getScientificFormat();
	private static Resources resources = GWT.create(Resources.class);
	
	public static String formatNumber(double v) {
		if (Math.abs(v) > 0.001) {
			return df.format(v);
		} else {
			return sf.format(v);
		}
	}

	public static HorizontalPanel mkHorizontalPanel() {
		return mkHorizontalPanel(false);
	}
	
	public static HorizontalPanel mkWidePanel() {
		HorizontalPanel r = mkHorizontalPanel(false);
		r.setWidth("100%");
		return r;
	}
	
	public static VerticalPanel mkTallPanel() {
		VerticalPanel r = mkVerticalPanel(false);
		r.setHeight("100%");
		return r;
	}
	
	public static HorizontalPanel mkHorizontalPanel(boolean spaced, Widget... widgets) {
		HorizontalPanel hp = new HorizontalPanel();		
//		hp.setStyleName("slightlySpaced");
		hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		if (spaced) {
			hp.setSpacing(4);
		}
		for (Widget w: widgets) {
			hp.add(w);
		}
		return hp;
	}

	public static VerticalPanel mkVerticalPanel() {
		return mkVerticalPanel(false);
	}
	
	public static VerticalPanel mkVerticalPanel(boolean spaced, Widget... widgets) {
		VerticalPanel vp = new VerticalPanel();
//		vp.setStyleName("slightlySpaced");
		vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		if (spaced) {
			vp.setSpacing(4);
		}
		
		for (Widget w: widgets) {
			vp.add(w);
		}
		return vp;
	}
	

	public static Label mkEmphLabel(String s) {
		Label r = new Label(s);
		r.setStyleName("emphasized");
		return r;
	}
	
	public static void floatLeft(Widget w) {
		w.getElement().getStyle().setFloat(Float.LEFT);
	}
	
	public static void floatLeft(FlowPanel fp, Widget w) {
		floatLeft(w);
		fp.add(w);
	}
	

	/**
	 * Colour: for example, MediumAquaMarine or LightSkyBlue
	 * 
	 * @param color
	 * @return
	 */
	public static Options createChartOptions(String... colors) {
		Options o = Options.create();
		o.setColors(colors);
		o.set("legend.position", "none");
		o.setLegend(LegendPosition.NONE);
		return o;
	}

	private static int lastX = -1, lastY = -1;
	public static void displayInPopup(String caption, Widget w) {
		displayInPopup(caption, w, false);
	}
	public static void displayInPopup(String caption, final Widget w, final boolean trackLocation) {
		final DialogBox db = new DialogBox(true, false) {
			@Override
			protected void endDragging(MouseUpEvent event) {
				super.endDragging(event);
				if (trackLocation) {
					lastX = getAbsoluteLeft();
					lastY = getAbsoluteTop();
				}
			}			
		};
		db.setText(caption);		
//		final PopupPanel pp = new PopupPanel(true, true);
		final DockPanel dp = new DockPanel();
//		HorizontalPanel hp = new HorizontalPanel();
//		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
//		Image i = new Image(resources.close());
//		i.addClickHandler(new ClickHandler() {			
//			@Override
//			public void onClick(ClickEvent event) {
//				db.hide();				
//			}
//		});
//		hp.add(i);		
//		hp.setWidth("100%");
		
//		dp.add(hp, DockPanel.NORTH);
		dp.add(w, DockPanel.CENTER);
		db.setWidget(dp);
		
		if (trackLocation) {
			db.setPopupPositionAndShow(displayAt(db, dp, w, lastX, lastY));
		} else {
			db.setPopupPositionAndShow(displayAt(db, dp, w, -1, -1));
		}

	}

	public static PositionCallback displayInCenter(final PopupPanel pp) {
		return displayAt(pp, null, null, -1, -1);
	}
	
	private static PositionCallback displayAt(final PopupPanel pp, final DockPanel dp, final Widget center,
			final int atX, final int atY) {
		return new PositionCallback() {			
			public void setPosition(int w, int h) {			
				if (h > Window.getClientHeight() - 100) {
					pp.setHeight((Window.getClientHeight() - 100) + "px");
					if (center != null && dp != null) {					
						dp.remove(center);					
						Widget scrl = makeScrolled(center);
						scrl.setHeight((Window.getClientHeight() - 120) + "px");
						dp.add(scrl, DockPanel.CENTER);					
					} else {				
						Widget wd = pp.getWidget();
						pp.setWidget(makeScrolled(wd));
					}			
					int useX = atX == -1 ? Window.getClientWidth() - w - 50 : atX;
					int useY = atY == -1 ? 50: atY;
					pp.setPopupPosition(useX, useY);					
				} else {
					int useX = atX == -1 ? Window.getClientWidth() - w - 50 : atX;
					int useY = atY == -1 ? Window.getClientHeight()/2 - h/2: atY;					
					pp.setPopupPosition(useX, useY);		
				}
			}
			};
	}
	
	public static ScrollPanel makeScrolled(Widget w) {
		ScrollPanel sp = new ScrollPanel(w);
		return sp;
	}
	
	public static String[] allCompounds(List<BarcodeColumn> columns) {
		List<String> r = new ArrayList<String>();
		for (BarcodeColumn dc : columns) {
			for (String c : dc.getCompounds()) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}

	public static Group findGroup(List<Group> groups, String title) {
		for (Group d : groups) {
			if (d.getName().equals(title)) {
				return d;
			}
		}
		return null;
	}

	public static Group groupFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (Barcode b : c.getBarcodes()) {
				if (b.getCode().equals(barcode)) {
					return c;						
				}
			}
		}
		return null;
	}
	
	public static List<Group> groupsFor(List<Group> columns, String barcode) {
		List<Group> r = new ArrayList<Group>();
		for (Group c : columns) {
			for (Barcode b : c.getBarcodes()) {
				if (b.getCode().equals(barcode)) {
					r.add(c);
					break;
				}
			}
		}
		return r;
	}

	public static String[] compoundsFor(List<Group> columns) {
		List<String> compounds = new ArrayList<String>();
		for (Group g : columns) {
			for (String c : g.getCompounds()) {
				if (!compounds.contains(c)) {
					compounds.add(c);
				}
			}
		}
		return compounds.toArray(new String[0]);
	}
	
	public static Barcode barcodeFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (Barcode b : c.getBarcodes()) {
				if (b.getCode().equals(barcode)) {
					return b;
				}
			}
		}
		return null;
	}
	
	public static Widget mkHelpButton(final TextResource helpText, 
			final ImageResource helpImage) {
		PushButton i = new PushButton(new Image(resources.help()));
		i.setStyleName("slightlySpaced");
		i.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				showHelp(helpText, helpImage);				
			}
			
		});
		return i;
	}

	public static void showHelp(TextResource helpText, ImageResource helpImage) {
		VerticalPanel vp = new VerticalPanel();				
		if (helpImage != null) {
			vp.add(new Image(helpImage));			
		}		
		SimplePanel sp = new SimplePanel();
		sp.setWidth("600px");
		sp.setWidget(new HTML(helpText.getText()));
		vp.add(sp);
		Utils.displayInPopup("Help", vp);
	}
	
	public static void ensureVisualisationAndThen(final Runnable r) {
		VisualizationUtils
		.loadVisualizationApi("1.1", r, "corechart");		
	}

	public static void setEnabled(HasWidgets root, boolean enabled) {
		for (Widget w: root) {
			if (w instanceof HasWidgets) {
				setEnabled((HasWidgets) w, enabled);
			}
			if (w instanceof FocusWidget) {
				((FocusWidget) w).setEnabled(enabled);
			}
		}
	}
}
