/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client.maintenance;

import java.util.Date;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import t.common.client.rpc.MaintenanceOperationsAsync;
import t.common.shared.maintenance.OperationResults;
import t.common.shared.maintenance.Progress;

/**
 * A widget that periodically polls and displays the current progress status from the server.
 * Automatically disappears when the task is complete.
 * 
 * @author johan
 */
public class ProgressDisplay extends Composite {
  final int POLL_INTERVAL = 2000; // ms

  Label statusLabel = new Label("0%");

  VerticalPanel logPanel;
  Button cancelButton, doneButton;

  Timer timer;
  private boolean cancelled = false;

  final MaintenanceOperationsAsync maintenanceService; 
      
  public ProgressDisplay(String taskName, MaintenanceOperationsAsync maintenanceService) {
    this.maintenanceService = maintenanceService;
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    statusLabel.addStyleName("taskStatus");
    vp.add(statusLabel);
    logPanel = new VerticalPanel();
    logPanel.addStyleName("taskLog");
    ScrollPanel sp = new ScrollPanel(logPanel);
    vp.add(sp);
    sp.setHeight("500px");
    sp.setWidth("500px");
    addLog("Begin operation: " + taskName);

    HorizontalPanel hp = new HorizontalPanel();
    hp.setSpacing(4);
    vp.add(hp);

    cancelButton = new Button("Cancel");
    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onCancel();
      }
    });
    hp.add(cancelButton);
    cancelButton.setEnabled(true);

    doneButton = new Button("Close");
    doneButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onDone();
      }
    });
    hp.add(doneButton);
    doneButton.setEnabled(false);

    timer = new Timer() {
      @Override
      public void run() {
        ProgressDisplay.this.maintenanceService.getProgress(new AsyncCallback<Progress>() {
          @Override
          public void onSuccess(Progress result) {
            setProgress(result);
          }

          @Override
          public void onFailure(Throwable caught) {
            addLog("Error: unable to obtain current job status from server");
            addLog(caught.getMessage());

          }
        });
      }
    };
    timer.scheduleRepeating(POLL_INTERVAL);
  }

  void setProgress(Progress p) {

    for (String m : p.getMessages()) {
      addLog(m);
    }

    if (p.isAllFinished()) {
      statusLabel.setText("All done (100%)");
      timer.cancel();
      cancelButton.setEnabled(false);
      doneButton.setEnabled(true);

      maintenanceService.getOperationResults(new AsyncCallback<OperationResults>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Error while obtaining operation results");
        }

        @Override
        public void onSuccess(OperationResults result) {
          int i = 0;

          if (cancelled) {
            logPanel.insert(infoLabel("* * * Operation cancelled * * *"), i++);
          } else if (result != null && result.successful()) {
            logPanel.insert(infoLabel("* * * Operation successful * * *"), i++);
          } else {
            logPanel.insert(infoLabel("* * * Operation failed * * *"), i++);
          }
          if (result != null) {
            for (String s : result.infoStrings()) {
              logPanel.insert(infoLabel(s), i++);
            }
          }
        }
      });
    } else {
      String task = p.getTask();
      statusLabel.setText(task + " (" + p.getPercentage() + "%)");
    }
  }

  void addLog(String message) {
    Date now = new Date();
    String time = DateTimeFormat.getFormat(PredefinedFormat.TIME_SHORT).format(now);

    Label l = new Label(time + " " + message);
    l.addStyleName("taskLogEntry");
    logPanel.insert(l, 0);
  }

  Label infoLabel(String message) {
    Label l = new Label(message);
    l.addStyleName("taskInfoEntry");
    return l;
  }

  void onCancel() {
    cancelButton.setEnabled(false);
    cancelled = true;
    maintenanceService.cancelTask(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        addLog("Unable to cancel task: " + caught.getMessage());
      }

      @Override
      public void onSuccess(Void result) {
        onCancelled();
      }
    });
  }

  protected void onDone() {}

  protected void onCancelled() {}

}
