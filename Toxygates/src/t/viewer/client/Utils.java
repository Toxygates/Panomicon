/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client;

import static t.common.client.Utils.makeScrolled;

import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;

import t.viewer.client.dialog.DialogPosition;

/**
 * GUI/GWT utility methods.
 * 
 * @author johan
 *
 */
public class Utils {
  public static String formatNumber(double v) {
    return t.common.client.Utils.formatNumber(v);    
  }

  public static HorizontalPanel mkHorizontalPanel() {
    return mkHorizontalPanel(false);
  }

  public static HorizontalPanel mkWidePanel() {
    HorizontalPanel panel = mkHorizontalPanel(false);
    panel.addStyleName("widePanel");
    return panel;
  }

  public static VerticalPanel mkTallPanel() {
    VerticalPanel panel = mkVerticalPanel(false);
    panel.addStyleName("tallPanel");
    return panel;
  }

  public static HorizontalPanel mkHorizontalPanel(boolean spaced, Widget... widgets) {
    HorizontalPanel panel = new HorizontalPanel();
    // hp.addStyleName("slightlySpaced");
    panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    if (spaced) {
      panel.setSpacing(4);
    }
    for (Widget w : widgets) {
      panel.add(w);
    }
    return panel;
  }

  public static HorizontalPanel wideCentered(Widget widget) {
    HorizontalPanel panel = mkWidePanel();
    panel.add(widget);
    return panel;
  }

  public static VerticalPanel mkVerticalPanel() {
    return mkVerticalPanel(false);
  }

  public static VerticalPanel mkVerticalPanel(boolean spaced, Widget... widgets) {
    VerticalPanel panel = new VerticalPanel();
    // vp.addStyleName("slightlySpaced");
    panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    if (spaced) {
      panel.setSpacing(4);
    }

    for (Widget widget : widgets) {
      panel.add(widget);
    }
    return panel;
  }

  public static Label mkEmphLabel(String string) {
    Label label = new Label(string);
    label.addStyleName("emphasized");
    return label;
  }

  public static void floatLeft(Widget widget) {
    widget.getElement().getStyle().setFloat(Float.LEFT);
  }

  public static void floatRight(Widget widget) {
    widget.getElement().getStyle().setFloat(Float.RIGHT);
  }

  public static void addAndFloatLeft(FlowPanel panel, Widget widget) {
    floatLeft(widget);
    panel.add(widget);
  }
  
  public static HTML linkTo(String url, String text) {
    HTML html = new HTML();
    html.setHTML("<a target=_blank href=\"" + url + "\">" + text + "</a>");
    return html;
  }

  /**
   * Open an URL 
   */
  public static void displayURL(String message, String linkText, final String url) {
    final DialogBox dialogBox = new DialogBox(false, true);

    dialogBox.setHTML(message);
    VerticalPanel panel = new VerticalPanel();
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    panel.addStyleName("widePanel");
    panel.add(linkTo(url, url));
    
    panel.add(new Button("Close", new ClickHandler() {
      @Override
      public void onClick(ClickEvent ev) {
        dialogBox.hide();
      }
    }));

    dialogBox.add(panel);
    displayInCenter(dialogBox);
  }

  private static int lastX = -1, lastY = -1;

  public static DialogBox displayInPopup(String caption, Widget widget, DialogPosition pos) {
    return displayInPopup(caption, widget, false, pos);
  }

  /**
   * Display a popup dialog.
   * 
   * @param caption Dialog title
   * @param widget Widget to show in dialog
   * @param trackLocation Whether to remember the location of this dialog box. Only one dialog box
   *        location can be remembered as we use static variables for this purpose. (note: we could
   *        fix by having a DialogContext or similar)
   * @param pos The position to display the dialog at.
   */
  public static DialogBox displayInPopup(String caption, final Widget widget,
      final boolean trackLocation, final DialogPosition pos) {
    DialogBox dialogBox = trackLocation ? new LocationTrackedDialog() : new DialogBox(true, false);
    return displayInPopup(dialogBox, caption, widget, trackLocation, pos);
  }

  /**
   * Display a popup dialog, as above.
   * 
   * @param dialogBox the dialog box to display the content in
   * @param caption Dialog title
   * @param widget Widget to show in dialog
   * @param trackLocation Whether to remember the location of this dialog box.
   * @param pos The position to display the dialog at.
   */
  public static DialogBox displayInPopup(DialogBox dialogBox, String caption, final Widget widget,
      final boolean trackLocation, final DialogPosition pos) {
    dialogBox.setText(caption);
    final DockPanel dockPanel = new DockPanel();
    dockPanel.add(widget, DockPanel.CENTER);
    dialogBox.setWidget(dockPanel);

    if (trackLocation) {
      deferredPositionAndShow(dialogBox, dockPanel, widget, lastX, lastY, pos, true);
    } else {
      deferredPositionAndShow(dialogBox, dockPanel, widget, -1, -1, pos, true);
    }
    return dialogBox;
  }

  public static class LocationTrackedDialog extends DialogBox {
    public LocationTrackedDialog() {
      super(true, false);
    }

    @Override
    protected void endDragging(MouseUpEvent event) {
      super.endDragging(event);
      lastX = getAbsoluteLeft();
      lastY = getAbsoluteTop();
    }
  }

  /**
   * Immediately shows a panel, and then schedules a deferred event that will
   * center it. 
   */
  public static void displayInCenter(final PopupPanel panel) {
    deferredPositionAndShow(panel, null, null, -1, -1, DialogPosition.Center, false);
  }

  /**
   * @param panel
   * @param dockPanel
   * @param dockPanelCenter
   * @param atX If not -1, this is the coordinate that is used
   * @param atY If not -1, this is the coordinate that is used
   * @param dialogPosition Used to compute coordinates if atX or atY is -1
   * @param deferShow If false, panel will be shown immediately. This is desirable
   * in cases where deferring panel.show() could result in race conditions.
   * @return
   */
  private static void deferredPositionAndShow(final PopupPanel panel, final DockPanel dockPanel,
      final Widget dockPanelCenter, final int atX, final int atY, final DialogPosition dialogPosition,
      boolean deferShow) {
    if (!deferShow) {
      panel.show();
    }
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        if (deferShow) {
          panel.show();
        }
        int height = panel.getOffsetHeight();
        if (DialogPosition.isTallDialog(height)) {
          // Have to make it scrolled, too tall
          panel.setHeight((Window.getClientHeight() - 100) + "px");
          if (dockPanelCenter != null && dockPanel != null) {
            dockPanel.remove(dockPanelCenter);
            Widget scrolled = makeScrolled(dockPanelCenter);
            scrolled.setHeight((Window.getClientHeight() - 120) + "px");
            dockPanel.add(scrolled, DockPanel.CENTER);
          } else {
            Widget widget = panel.getWidget();
            panel.setWidget(makeScrolled(widget));
          }
        }
        positionPanel(panel, dialogPosition, atX, atY);
      }
    });  
  }
  
  public static void positionPanel(PopupPanel panel, DialogPosition dialogPosition, int atX, int atY) {
    int width =  panel.getOffsetWidth();
    int height = panel.getOffsetHeight();
    panel.setPopupPosition(atX != -1 ? atX : dialogPosition.computeX(width), atY != -1 ? atY : dialogPosition.computeY(height));
    panel.setWidth("auto");
  }
  
  public static void positionPanel(PopupPanel panel, DialogPosition dialogPosition) {
    positionPanel(panel, dialogPosition, -1, -1);
  }

  public static void showHelp(TextResource helpText, ImageResource helpImage) {
    VerticalPanel verticalPanel = new VerticalPanel();
    if (helpImage != null) {
      HorizontalPanel widePanel = Utils.mkWidePanel();
      widePanel.add(new Image(helpImage));
      verticalPanel.add(widePanel);
    }
    SimplePanel simplePanel = new SimplePanel();
    simplePanel.setWidget(new HTML(helpText.getText()));
    verticalPanel.add(simplePanel);
    simplePanel.addStyleName("helpPanelInner");
    verticalPanel.addStyleName("helpPanelOuter");    
    Utils.displayInPopup("Help", verticalPanel, DialogPosition.Center);
  }

  public static void loadHTML(String url, RequestCallback callback) {
    try {
      RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
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
    VisualizationUtils.loadVisualizationApi(r, CoreChart.PACKAGE);
  }

  public static void setEnabled(HasWidgets root, boolean enabled) {
    for (Widget widget : root) {
      if (widget instanceof HasWidgets) {
        setEnabled((HasWidgets) widget, enabled);
      }
      if (widget instanceof FocusWidget) {
        ((FocusWidget) widget).setEnabled(enabled);
      }
    }
  }

  public interface Templates extends SafeHtmlTemplates {

    @Template("<div title=\"{0}\">")
    SafeHtml startToolTip(String toolTipText);

    @Template("</div>")
    SafeHtml endToolTip();
    
    @Template("<div class=\"{0}\">")
    SafeHtml startStyled(String style);
    
    @Template("</div>")
    SafeHtml endStyled();    
  }

  public static DialogBox waitDialog() {
    DialogBox waitDialog = new DialogBox(false, true);
    waitDialog.setWidget(Utils.mkEmphLabel("Please wait..."));
    return waitDialog;
  }
  
  public static SafeHtml tooltipSpan(String tooltip, String text) {
      return SafeHtmlUtils.fromSafeConstant("<span title=\"" + tooltip + "\">" + text + "</span>");
  }

  public static void inject(final List<String> p_jsList, Logger logger, final Runnable callback) {
    final String js = GWT.getModuleBaseForStaticFiles() + p_jsList.remove(0);

    ScriptInjector.fromUrl(js).setCallback(new Callback<Void, Exception>() {
      @Override
      public void onFailure(Exception e) {
        logger.severe("Script load failed. (" + js + ")");
      }

      @Override
      public void onSuccess(Void ok) {
        //Injected all scripts
        if (!p_jsList.isEmpty()) {
          inject(p_jsList, logger, callback);
        } else {
          callback.run();
        }
      }
    }).setWindow(ScriptInjector.TOP_WINDOW).inject();
  }
  
  /**
   * Finds the index of the first item in a ListBox with the given name
   * @param listBox the listBox whose items should be searched
   * @param itemName the name of the item to find 
   * @return the index of the item if found, otherwise -1
   */
  public static int findListBoxItemIndex(ListBox listBox, String itemName) {
    for (int i = 0; i < listBox.getItemCount(); i++) {
      if (listBox.getItemText(i) == itemName) {
        return i;
      }
    }
    return -1;
  }
}
