package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.ui.Composite;

/**
 * A Composite that is also a DataViewListener.
 * Has default implementations for the change listener methods.
 * @author johan
 *
 */
class DataListenerWidget extends Composite implements DataViewListener {

	private List<DataViewListener> listeners = new ArrayList<DataViewListener>();
	
	protected DataFilter chosenDataFilter;
	protected String chosenProbe;
	protected List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	
	public DataListenerWidget() {
		
	}
	
	public void addListener(DataViewListener l) {
		listeners.add(l);
	}
	
	//incoming signals
	public void dataFilterChanged(DataFilter filter) {
		chosenDataFilter = filter;
		changeDataFilter(filter);
	}
	
	public void probeChanged(String probe) {
		chosenProbe = probe;
		changeProbe(probe);
	}
	
	public void compoundsChanged(List<String> compounds) {
		chosenCompounds = compounds;
		changeCompounds(compounds);
	}
	
	public void compoundChanged(String compound) {
		chosenCompound = compound;
		changeCompound(compound);
	}
	
	public void valueTypeChanged(ValueType type) {
		chosenValueType = type;
		changeValueType(type);
	}
	
	//outgoing signals	
	protected void changeDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
		for (DataViewListener l : listeners) {
			l.dataFilterChanged(filter);
		}		
	}
	
	protected void changeProbe(String probe) {
		chosenProbe = probe;
		for (DataViewListener l: listeners) {
			l.probeChanged(probe);
		}
	}
	
	protected void changeCompounds(List<String> compounds) {
		chosenCompounds = compounds;
		for (DataViewListener l: listeners) {
			l.compoundsChanged(compounds);
		}
	}
	
	protected void changeCompound(String compound) {
		chosenCompound = compound;
		for (DataViewListener l : listeners) {
			l.compoundChanged(compound);
		}
	}
	
	protected void changeValueType(ValueType type) {
		chosenValueType = type;
		for (DataViewListener l : listeners) {
			l.valueTypeChanged(type);
		}
	}
}	
