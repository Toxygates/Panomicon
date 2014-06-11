package t.admin.client;

import t.admin.shared.Progress;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
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
	String lastTask = "";
	
	VerticalPanel logPanel;
	
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
	}
	
	void setProgress(Progress p) {
		String task = p.getTask();
		statusLabel.setText(task + " (" + p.getPercentage() + "%)");
		
		if (!task.equals(lastTask)) {
			addLog("Finished: " + lastTask);
			lastTask = task;
		}
		
		if (p.getPercentage() == 100) {
			onDone();
		}
	}
	
	void addLog(String message) {
		Label l = new Label(message);
		l.setStylePrimaryName("taskLogEntry");		
		logPanel.insert(l, 0);
	}
	
	protected void onCancel() {
		
	}
	
	protected void onDone() {
		
	}
	
}
