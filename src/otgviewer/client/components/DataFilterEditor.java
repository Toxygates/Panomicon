package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import otgviewer.client.rpc.SparqlService;
import otgviewer.client.rpc.SparqlServiceAsync;
import otgviewer.shared.DataFilter;
import t.common.shared.SampleClass;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class DataFilterEditor extends DataListenerWidget {
	List<SampleClass> sampleClasses;	
	final SCListBox organismSelector, organSelector, testTypeSelector, repTypeSelector;
	private final SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	protected final Logger logger;
	
	class SCListBox extends ListBox {
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
			setItems(asList(SampleClass.collect(scs, key)));		
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

	public DataFilterEditor() {
		HorizontalPanel hp = new HorizontalPanel();
		initWidget(hp);
		logger = Utils.getLogger("dfeditor");
		
		sparqlService.sampleClasses(new AsyncCallback<SampleClass[]>() {
			
			@Override
			public void onSuccess(SampleClass[] result) {
				setAvailable(result);		
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to obtain sample classes from server");				
			}
		});
		
		
		organismSelector = new SCListBox();
		organSelector = new SCListBox();
		testTypeSelector = new SCListBox();
		repTypeSelector = new SCListBox();
		
		hp.add(organismSelector);
		hp.add(organSelector);
		hp.add(testTypeSelector);
		hp.add(repTypeSelector);
	
		organismSelector.addChangeHandler(new ChangeHandler() {			
			@Override
			public void onChange(ChangeEvent event) {
				changeOrganism();				
			}
		});
		
		organSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				changeOrgan();				
			}
		});

		testTypeSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				changeTestType();
			}
		});

		repTypeSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				propagate();
			}
		});
	}
	
	private void changeOrganism() {
		String sel = organismSelector.getSelected();
		if (sel != null) {
			List<SampleClass> selected = constrain1(sel);
			organSelector.setItemsFrom(selected, "organ_id");
		}			
		changeOrgan();
	}
	
	private void changeOrgan() {
		String sel1 = organismSelector.getSelected();
		String sel2 = organSelector.getSelected();
		if (sel1 != null && sel2 != null) {
			List<SampleClass> selected = constrain2(sel1, sel2);
			testTypeSelector.setItemsFrom(selected, "test_type");
		}
		changeTestType();
	}
	
	private void changeTestType() {
		String sel1 = organismSelector.getSelected();
		String sel2 = organSelector.getSelected();
		String sel3 = testTypeSelector.getSelected();
		if (sel1 != null && sel2 != null && sel3 != null) {
			List<SampleClass> selected = constrain3(sel1, sel2, sel3);
			repTypeSelector.setItemsFrom(selected, "sin_rep_type");
		}
		propagate();
	}
	
	private List<String> asList(Set<String> xs) {
		ArrayList<String> r = new ArrayList<String>(xs);
		return r;
	}
	
	private void propagate() {
		String sel1 = organismSelector.getSelected();
		String sel2 = organSelector.getSelected();
		String sel3 = testTypeSelector.getSelected();
		String sel4 = repTypeSelector.getSelected();
		if (sel1 != null && sel2 != null && sel3 != null && sel4 != null) {
			SampleClass r = new SampleClass();						
			r.put("organism", sel1);
			r.put("organ_id", sel2);
			r.put("test_type", sel3);
			r.put("sin_rep_type", sel4);
			logger.info("Propagate change to " + r.toString());
			try {
//				changeDataFilter(r.asDataFilter());
				changeSampleClass(r);
			} catch (IllegalArgumentException iae) {				
				logger.warning("Illegal argument (unable to parse " + r + ")");
				logger.log(Level.WARNING, "Unable to parse", iae);
				//bad data
			}
		}
	}
	
	public void setAvailable(SampleClass[] sampleClasses) {
		logger.info("Received " + sampleClasses.length + " sample classes");
		this.sampleClasses = Arrays.asList(sampleClasses);	
		organismSelector.setItemsFrom(this.sampleClasses, "organism");		
		organSelector.setItemsFrom(this.sampleClasses, "organ_id");		
		testTypeSelector.setItemsFrom(this.sampleClasses, "test_type");		
		repTypeSelector.setItemsFrom(this.sampleClasses, "sin_rep_type");
		changeOrgan(); 
	}
	
	public List<SampleClass> constrain1(String organism) {
		return SampleClass.filter(sampleClasses, "organism", organism);
	}
	
	public List<SampleClass> constrain2(String organism, String organ) {
		return SampleClass.filter(constrain1(organism), "organ_id", organ);
	}
	
	public List<SampleClass> constrain3(String organism, String organ, String testType) {
		return SampleClass.filter(constrain2(organism, organ), "test_type", testType);
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		//do NOT call superclass method. Prevent signal from being passed on.
//		chosenDataFilter = filter;
		
//		//TODO update
//		organismSelector.trySelect(filter.organism.toString());
//		organSelector.trySelect(filter.organ.toString());
//		cellTypeSelector.trySelect(filter.cellType.toString());
//		repeatTypeSelector.trySelect(filter.repeatType.toString());
	}
	
	@Override
	public void sampleClassChanged(SampleClass sc) {
		//do NOT call superclass method. Prevent signal from being passed on.
		chosenSampleClass = sc;		
		
		//TODO update
		organismSelector.trySelect(sc.get("organism"));
		organSelector.trySelect(sc.get("organ_id"));
		testTypeSelector.trySelect(sc.get("test_type"));
		repTypeSelector.trySelect(sc.get("sin_rep_type"));
	}
	
	
}
