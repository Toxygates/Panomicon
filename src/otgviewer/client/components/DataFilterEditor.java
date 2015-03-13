package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import t.common.client.rpc.SparqlService;
import t.common.client.rpc.SparqlServiceAsync;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

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
		for (int i = 0; i <= sel; ++i) {
			String sval = selectors[i].getSelected();
			if (sval != null) {
				selected = SampleClass.filter(selected, parameters[i], sval);
			}
		}
		for (int i = sel+1; i < selectors.length; ++i) {
			selectors[i].setItemsFrom(selected, parameters[i]);
		}
		
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
	
	public DataFilterEditor(DataSchema schema) {
		HorizontalPanel hp = new HorizontalPanel();
		initWidget(hp);
		logger = Utils.getLogger("dfeditor");
		
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
		for (int i = 0; i < selectors.length; ++i) {
			selectors[i].setItemsFrom(this.sampleClasses, parameters[i]);
		}				
		changeFrom(0);
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
