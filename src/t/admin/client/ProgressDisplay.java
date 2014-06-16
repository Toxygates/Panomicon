package t.admin.client;

import java.util.Date;

import t.admin.shared.Progress;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.ibm.icu.util.Calendar;

/**
 * A widget that periodically polls and displays the current progress status from the server.
 * Automatically disappears when the task is complete. 
 * @author johan
 */
public class ProgressDisplay extends Composite {
	final int POLL_INTERVAL = 2000; //ms
	
	Label statusLabel = new Label("0%");
	
	VerticalPanel logPanel;
	
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
		sp.setHeight("200px");
		sp.setWidth("500px");
		addLog("Begin task: " + taskName);
		
		Button b = new Button("Cancel");
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				onCancel();			
			}
		});
		vp.add(b);
		
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
		String task = p.getTask();
		statusLabel.setText(task + " (" + p.getPercentage() + "%)");
		
		for (String m: p.getMessages()) {
			addLog(m);
		}
		
		if (p.isAllFinished()) {
			onDone();
			//TODO should check for exception here as well?
			statusLabel.setText("All finished (100%)");
			timer.cancel();
		}
	}
	
	void addLog(String message) {
		Date now = new Date();
		String time = DateTimeFormat.getFormat(PredefinedFormat.TIME_SHORT).format(now);
		
		Label l = new Label(time + " " + message);
		l.setStylePrimaryName("taskLogEntry");		
		logPanel.insert(l, 0);
	}
	
	protected void onCancel() {
		maintenanceService.cancelTask(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				addLog("Unable to cancel task: " + caught.getMessage());				
			}

			@Override
			public void onSuccess(Void result) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
	
	protected void onDone() {
		
	}
	
}
