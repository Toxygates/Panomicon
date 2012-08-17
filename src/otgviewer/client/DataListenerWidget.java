package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.DataColumn;
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
	protected String[] chosenProbes;
	protected List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	protected List<DataColumn> chosenColumns = new ArrayList<DataColumn>();
	
	protected boolean active = false;
	
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
	
	public void probesChanged(String[] probes) {
		chosenProbes = probes;
		changeProbes(probes);
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
	
	public void columnsChanged(List<DataColumn> columns) {
		chosenColumns = columns;
		changeColumns(columns);
	}
	
	public void heightChanged(int newHeight) {
		changeHeight(newHeight);
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
	
	protected void changeProbes(String[] probes) {
		chosenProbes = probes;
		for (DataViewListener l: listeners) {
			l.probesChanged(probes);
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
	
	
	protected void changeColumns(List<DataColumn> columns) {
		chosenColumns = columns;
		for (DataViewListener l : listeners) {
			l.columnsChanged(columns);
		}
	}
	protected void changeHeight(int newHeight) {
		for (DataViewListener l : listeners) {
			l.heightChanged(newHeight);
		}
	}
	
	// other
	
	public void activate() {
		active = true;
	}
	
	public void deactivate() {
		active = false;
	}
	
	
}	
