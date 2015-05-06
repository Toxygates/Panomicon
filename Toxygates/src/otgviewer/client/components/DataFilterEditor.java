package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.viewer.client.rpc.SparqlService;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.DataSchema;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class DataFilterEditor extends DataListenerWidget {
	List<SampleClass> sampleClasses;	
	final SCListBox[] selectors;	
	private final SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	private final String[] parameters;
	protected final Logger logger;
	
	class SCListBox extends ListBox {
		int idx;
		SCListBox(int idx) {			
			this.idx = idx;			
		}
		
		void setItems(List<String> items) {
			String oldSel = getSelected();
			clear();
			for (String i: items) {
				addItem(i);
			}
			
			if (oldSel != null && items.indexOf(oldSel) != -1) {
				trySelect(oldSel);
			} else if (items.size() > 0) {
				setSelectedIndex(0);
			}			
		}
		
		void trySelect(String item) {
			for (int i = 0; i < getItemCount(); i++) {
				if (getItemText(i).equals(item)) {
					setSelectedIndex(i);
					return;
				}
			}
		}
		
		void setItemsFrom(List<SampleClass> scs, String key) {
			setItems(new ArrayList<String>(SampleClass.collect(scs, key)));		
		}
				
		String getSelected() {
			int i = getSelectedIndex();
			if (i != -1) {
				return getItemText(i);
			} else {
				return null;
			}
		}		
	}

	void changeFrom(int sel) {
		List<SampleClass> selected = sampleClasses;
		//Get the selected values on the left of, and including, this one
		for (int i = 0; i <= sel; ++i) {
			String sval = selectors[i].getSelected();
			if (sval != null) {
				selected = SampleClass.filter(selected, parameters[i], sval);
				logger.info("Filtered to " + selected.size());
			}
		}
		//Constrain the selectors to the right of this one
		for (int i = sel+1; i < selectors.length; ++i) {
			selectors[i].setItemsFrom(selected, parameters[i]);
		}
	
		if (sel < selectors.length - 1) {
			changeFrom(sel + 1);
		} else if (sel == selectors.length - 1) {
			SampleClass r = new SampleClass();
			boolean allSet = true;
			for (int i = 0; i < selectors.length; ++i) {
				String x = selectors[i].getSelected();
				if (x == null) {
					allSet = false;
				} else {
					r.put(parameters[i], x);
				}
			}
			
			if (allSet) {
				logger.info("Propagate change to " + r.toString());			
				changeSampleClass(r);
			}		
		}
	}
	
	public DataFilterEditor(DataSchema schema) {
		HorizontalPanel hp = new HorizontalPanel();
		initWidget(hp);
		logger = SharedUtils.getLogger("dfeditor");
		
		update();
		
		parameters = schema.macroParameters();
		selectors = new SCListBox[parameters.length];
		for (int i = 0; i < parameters.length; ++i) {
			selectors[i] = new SCListBox(i);
			hp.add(selectors[i]);
			final int sel = i;
			selectors[i].addChangeHandler(new ChangeHandler() {				
				@Override
				public void onChange(ChangeEvent event) {
					changeFrom(sel);					
				}
			});
			
		}			
	}
	
	public void update() {
		sparqlService.sampleClasses(new PendingAsyncCallback<SampleClass[]>(this, 
				"Unable to obtain sample classes from server") {			
			@Override
			public void handleSuccess(SampleClass[] result) {
				setAvailable(result);		
			}					
		});
	}

	public void setAvailable(SampleClass[] sampleClasses) {
		logger.info("Received " + sampleClasses.length + " sample classes");
		if (sampleClasses.length > 0) {
			logger.info(sampleClasses[0].toString() + " ...");
		}
		this.sampleClasses = Arrays.asList(sampleClasses);			
		selectors[0].setItemsFrom(this.sampleClasses, parameters[0]);						
		changeFrom(0); //Propagate the constraint
	}
	
	@Override
	public void sampleClassChanged(SampleClass sc) {
		//do NOT call superclass method. Prevent signal from being passed on.
		chosenSampleClass = sc;				
		
		for (int i = 0; i < selectors.length; ++i) {
			selectors[i].trySelect(sc.get(parameters[i]));
		}	
	}	
}
