package t.admin.client;

import java.util.Date;

import t.admin.shared.OperationResults;
import t.admin.shared.Progress;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A widget that periodically polls and displays the current progress status from the server.
 * Automatically disappears when the task is complete. 
 * @author johan
 */
public class ProgressDisplay extends Composite {
	final int POLL_INTERVAL = 2000; //ms
	
	Label statusLabel = new Label("0%");
	
	VerticalPanel logPanel;
	Button cancelButton, doneButton;
	
	Timer timer;
	
	final MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	public ProgressDisplay(String taskName) {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		statusLabel.setStylePrimaryName("taskStatus");
		vp.add(statusLabel);
		logPanel = new VerticalPanel();
		logPanel.setStylePrimaryName("taskLog");
		ScrollPanel sp = new ScrollPanel(logPanel);
		vp.add(sp);
		sp.setHeight("500px");
		sp.setWidth("500px");
		addLog("Begin task: " + taskName);
		
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
				maintenanceService.getProgress(new AsyncCallback<Progress>() {					
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
	
		for (String m: p.getMessages()) {
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
					
					if (result != null && result.successful()) {
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
		l.setStylePrimaryName("taskLogEntry");		
		logPanel.insert(l, 0);
	}
	
	Label infoLabel(String message) {
		Label l = new Label(message);
		l.setStylePrimaryName("taskInfoEntry");
		return l;
	}
	
	void onCancel() {
		cancelButton.setEnabled(false);
		doneButton.setEnabled(true);
		maintenanceService.cancelTask(new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				addLog("Unable to cancel task: " + caught.getMessage());				
			}

			@Override
			public void onSuccess(Void result) { }			
		});
	}
	
	protected void onDone() {
		
	}
	
}
