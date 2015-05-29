/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import otgviewer.client.charts.google.GVizCharts;
import otgviewer.client.dialog.DialogPosition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
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

/**
 * GUI/GWT utility methods.
 * @author johan
 *
 */
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
	
	private static Resources resources = GWT.create(Resources.class);

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
//		hp.setStylePrimaryName("slightlySpaced");
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
	
	public static HorizontalPanel wideCentered(Widget w) {
		HorizontalPanel hp = mkWidePanel();
		hp.add(w);
		return hp;
	}

	public static VerticalPanel mkVerticalPanel() {
		return mkVerticalPanel(false);
	}
	
	public static VerticalPanel mkVerticalPanel(boolean spaced, Widget... widgets) {
		VerticalPanel vp = new VerticalPanel();
//		vp.setStylePrimaryName("slightlySpaced");
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
		r.setStylePrimaryName("emphasized");
		return r;
	}
	
	public static void floatLeft(Widget w) {
		w.getElement().getStyle().setFloat(Float.LEFT);
	}
	
	public static void floatRight(Widget w) {
		w.getElement().getStyle().setFloat(Float.RIGHT);
	}
	
	public static void floatLeft(FlowPanel fp, Widget w) {
		floatLeft(w);
		fp.add(w);
	}

	/**
	 * Open an URL in a new window or tab. 
	 * @param message
	 * @param buttonText
	 * @param url
	 */
	public static void urlInNewWindow(String message, String buttonText, final String url) {
		final DialogBox db = new DialogBox(false, true);							
		
		db.setHTML(message);				
		HorizontalPanel hp = new HorizontalPanel();
		
		hp.add(new Button(buttonText, new ClickHandler() {
			public void onClick(ClickEvent ev) {
				// Note that some browsers (e.g. Safari 3.1) are extremely careful in their
				// handling of this call. If it is not directly inside a button's ClickHandler
				// it will be ignored.
				Window.open(url, "_blank", "");
				db.hide();
			}
		}));
		
		hp.add(new Button("Cancel", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				db.hide();								
			}
		}));
		
		db.add(hp);
		db.setPopupPositionAndShow(displayInCenter(db));						
	}

	private static int lastX = -1, lastY = -1;
	public static DialogBox displayInPopup(String caption, Widget w, DialogPosition pos) {
		return displayInPopup(caption, w, false, pos);
	}
	
	/**
	 * Display a popup dialog.
	 * @param caption Dialog title
	 * @param w Widget to show in dialog
	 * @param trackLocation Whether to remember the location of this dialog box. Only one dialog box
	 * location can be remembered as we use static variables for this purpose. (TODO: fix by having
	 * a DialogContext or similar)
	 * @pos The position to display the dialog at.
	 */
	public static DialogBox displayInPopup(String caption, final Widget w, final boolean trackLocation,
			final DialogPosition pos) {
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
		final DockPanel dp = new DockPanel();
		dp.add(w, DockPanel.CENTER);
		db.setWidget(dp);
		
		if (trackLocation) {
			db.setPopupPositionAndShow(displayAt(db, dp, w, lastX, lastY, pos));
		} else {
			db.setPopupPositionAndShow(displayAt(db, dp, w, -1, -1, pos));
		}
		return db;
	}

	public static PositionCallback displayInCenter(final PopupPanel pp) {
		return displayAt(pp, null, null, -1, -1, DialogPosition.Center);
	}
	
	/**
	 * 
	 * @param pp
	 * @param dp
	 * @param center
	 * @param atX If not -1, this is the coordinate that is used
	 * @param atY If not -1, this is the coordinate that is used
	 * @param pos Used to compute coordinates if atX or atY is -1
	 * @return
	 */
	private static PositionCallback displayAt(final PopupPanel pp, final DockPanel dp, 
			final Widget center, final int atX, final int atY, final DialogPosition pos) {
		return new PositionCallback() {			
			public void setPosition(int w, int h) {			
				if (DialogPosition.isTallDialog(h)) {
					// Have to make it scrolled, too tall
					pp.setHeight((Window.getClientHeight() - 100) + "px");
					if (center != null && dp != null) {					
						dp.remove(center);					
						Widget scrl = makeScrolledSize(center, w);
						scrl.setHeight((Window.getClientHeight() - 120) + "px");
						dp.add(scrl, DockPanel.CENTER);					
					} else {				
						Widget wd = pp.getWidget();
						pp.setWidget(makeScrolledSize(wd, w));
					}			
				}				
				pp.setPopupPosition(atX != -1 ? atX : pos.computeX(w), 
						atY != -1 ? atY : pos.computeY(h));
				pp.setWidth("auto");
			}
			};
	}
	
	public static ScrollPanel makeScrolled(Widget w) {
		ScrollPanel sp = new ScrollPanel(w);
		sp.setWidth("auto");
		return sp;
	}
	
	public static ScrollPanel makeScrolledSize(Widget w, int width) {
		ScrollPanel sp = makeScrolled(w);
		sp.setWidth("auto");
//		sp.setWidth(width + "px");		
		return sp;
	}
	
	public static Widget mkHelpButton(final TextResource helpText, 
			final ImageResource helpImage) {
		PushButton i = new PushButton(new Image(resources.help()));
		i.setStylePrimaryName("slightlySpaced");
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
			HorizontalPanel wp = Utils.mkWidePanel();
			wp.add(new Image(helpImage));		
			vp.add(wp);
		}		
		SimplePanel sp = new SimplePanel();	
		sp.setWidget(new HTML(helpText.getText()));
		vp.add(sp);
		sp.setWidth("600px");
		if (helpImage != null) {
			vp.setWidth((helpImage.getWidth() + 50) + "px");
		} else {
			vp.setWidth("650px");
		}
		Utils.displayInPopup("Help", vp, DialogPosition.Center);
	}
	
	public static void loadHTML(String url, RequestCallback callback) {
		try {
			RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
					URL.encode(url));
			rb.sendRequest(null, callback);
		} catch (RequestException e) {
			Window.alert("Server communication error when attempting to get " + url);
		}
	}
	
	public abstract static class HTMLCallback implements RequestCallback {
		
		@Override
		public void onResponseReceived(Request request, Response response) {
			setHTML(response.getText());
		}
		
		abstract protected void setHTML(String html);

		@Override
		public void onError(Request request, Throwable exception) {
			Window.alert("Server communication error.");
		}		
	}
	
	public static void ensureVisualisationAndThen(final Runnable r) {
		GVizCharts.loadAPIandThen(r);				
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
	
	public interface Templates extends SafeHtmlTemplates {

		@Template("<div title=\"{0}\">")
		SafeHtml startToolTip(String toolTipText);

		@Template("</div>")
		SafeHtml endToolTip();
	}
	
	public static DialogBox waitDialog() {
		DialogBox waitDialog = new DialogBox(false, true);
		waitDialog.setWidget(Utils.mkEmphLabel("Please wait..."));		
		waitDialog.setPopupPositionAndShow(Utils.displayInCenter(waitDialog));		
		return waitDialog;
	}
}
