package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import static otgviewer.client.components.StorageParser.*;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.OTGUtils;
import otgviewer.shared.ValueType;
import bioweb.shared.array.DataColumn;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * A Composite that is also a DataViewListener.
 * Has default implementations for the change listener methods.
 * 
 * @author johan
 *
 */
public class DataListenerWidget extends Composite implements DataViewListener {

	private List<DataViewListener> listeners = new ArrayList<DataViewListener>();
	
	public DataFilter chosenDataFilter; //TODO
	protected String[] chosenProbes = new String[0];
	protected List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	protected List<Group> chosenColumns = new ArrayList<Group>();
	protected BarcodeColumn chosenCustomColumn;
	
	private StorageParser parser;
	
	public List<Group> chosenColumns() { return this.chosenColumns; }
	
	public DataListenerWidget() {
		super();
	}
	
	public void addListener(DataViewListener l) {
		listeners.add(l);
	}
	
	//incoming signals
	public void dataFilterChanged(DataFilter filter) {		
		chosenDataFilter = filter;
		changeDataFilter(filter);
	}

	public void probesChanged(String[] probes) {
		chosenProbes = probes;
		changeProbes(probes);
	}
	
	public void availableCompoundsChanged(List<String> compounds) {
		changeAvailableCompounds(compounds);
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
	
	public void columnsChanged(List<Group> columns) {
		chosenColumns = columns;		
		changeColumns(columns);
	}
	
	public void customColumnChanged(BarcodeColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		changeCustomColumn(customColumn);
	}

	//outgoing signals	
	protected void changeDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
		for (DataViewListener l : listeners) {
			l.dataFilterChanged(filter);
		}		
	}
	
	protected void changeProbes(String[] probes) {
		chosenProbes = probes;
		for (DataViewListener l: listeners) {
			l.probesChanged(probes);
		}
	}
	
	/**
	 * Change the available compounds
	 * @param compounds
	 */
	protected void changeAvailableCompounds(List<String> compounds) {
		for (DataViewListener l: listeners) {
			l.availableCompoundsChanged(compounds);
		}
	}
	
	/** 
	 * Change the selected compounds
	 * @param compounds
	 */
	protected void changeCompounds(List<String> compounds) {
		chosenCompounds = compounds;
		assert(compounds != null);
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
	
	protected void changeColumns(List<Group> columns) {
		chosenColumns = columns;
		assert(columns != null);
		for (DataViewListener l : listeners) {
			l.columnsChanged(columns);
		}
	}
	
	protected void changeCustomColumn(BarcodeColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		for (DataViewListener l: listeners) {
			l.customColumnChanged(customColumn);
		}
	}
	
	public void propagateTo(DataViewListener other) {
		other.dataFilterChanged(chosenDataFilter);
		other.probesChanged(chosenProbes);
		other.compoundsChanged(chosenCompounds);
		other.compoundChanged(chosenCompound);
		other.valueTypeChanged(chosenValueType);
		other.columnsChanged(chosenColumns);		
		other.customColumnChanged(chosenCustomColumn);
	}

	protected Storage tryGetStorage() {
		Storage r = Storage.getLocalStorageIfSupported();
		// TODO concurrency an issue for GWT here?
		if (r == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} 
		return r;
	}
	
	protected String keyPrefix(Screen s) {
		// TODO use enum
		String uit = s.manager().getUIType();
		if (uit.equals("toxygates")) {
			return "OTG";
		} else {
			return "Toxy_" + uit;
		}
	}
	
	protected StorageParser getParser(Screen s) {
		if (parser != null) {
			return parser;
		}
		parser = new StorageParser(tryGetStorage(), keyPrefix(s));
		return parser;
	}
	
	/**
	 * Store this widget's state into local storage.
	 */
	public void storeState(Screen s) {
		StorageParser p = getParser(s);
		storeState(p);		
	}
	
	/**
	 * Store this widget's state into local storage.
	 */
	public void storeState(StorageParser p) {
		storeDataFilter(p);
		storeColumns(p);
		storeProbes(p);
	}
		
	public void storeDataFilter(StorageParser p) {	
		if (chosenDataFilter != null) {
			p.setItem("dataFilter", packDataFilter(chosenDataFilter));			
		} else {
			p.clearItem("dataFilter");
		}			
	}
	
	protected void storeColumns(StorageParser p, String key, Collection<BarcodeColumn> columns) {				
		if (chosenDataFilter != null) {
			key = key + "." + packDataFilter(chosenDataFilter);
			if (!columns.isEmpty()) {
				p.setItem(key, packColumns(columns));
			} else {
				p.clearItem(key);				
			}
		}		
	}
	
	public void storeColumns(StorageParser p) {
		storeColumns(p, "columns", OTGUtils.asColumns(chosenColumns));
	}	
	
	protected void storeCustomColumn(StorageParser p, DataColumn<?> column) {		
		if (column != null) {
			p.setItem("customColumn", column.pack());
		} else {
			p.clearItem("customColumn");
		}		
	}

	protected List<Group> loadColumns(StorageParser p, String key,
			Collection<BarcodeColumn> expectedColumns) throws Exception {
		String v = p.getItem(key + "." + packDataFilter(chosenDataFilter));
		List<Group> r = new ArrayList<Group>();
		if (v != null && !v.equals(packColumns(expectedColumns))) {
			String[] spl = v.split("###");
			for (String cl : spl) {
				Group c = (Group) unpackColumn(cl);
				r.add(c);
			}
			return r;
		}
		return null;
	}

	public void storeProbes(StorageParser p) {
		p.setItem("probes", packProbes(chosenProbes));	
	}

	/**
	 * Load saved state from the local storage.
	 * If the loaded state is different from what was previously remembered in this widget, the appropriate 
	 * signals will fire.
	 */
	public void loadState(Screen sc) {
		StorageParser p = getParser(sc);		
		loadState(p);
	}

	public void loadState(StorageParser p) {
		DataFilter nf = unpackDataFilter(p.getItem("dataFilter")); 
		if (nf != null && (chosenDataFilter == null || !chosenDataFilter.equals(nf))) { 			
			dataFilterChanged(nf);
		}
		if (chosenDataFilter != null) {				
			try {
				List<Group> cs = loadColumns(p, "columns", OTGUtils.asColumns(chosenColumns));					
				if (cs != null) {						
					columnsChanged(cs);
				}						
				BarcodeColumn cc = unpackColumn(p.getItem("customColumn"));
				if (cc != null) {																		
					customColumnChanged(cc);						
				}
			} catch (Exception e) {										
				//one possible failure source is if data is stored in an incorrect format
				columnsChanged(new ArrayList<Group>());
				storeColumns(p); //overwrite the old data
				storeCustomColumn(p, null); //ditto
			}

		}
		String v = p.getItem("probes");			
		if (v != null && !v.equals("") && !v.equals(packProbes(chosenProbes))) {
			chosenProbes = v.split("###");				
			probesChanged(chosenProbes);				
		} else if (v == null || v.equals("")) {
			probesChanged(new String[0]);
		}
	}
	
//	public void clearState() {	
//	}
//	
	private int numPendingRequests = 0;
	
	private DialogBox waitDialog;
	
	// Load indicator handling
	protected void addPendingRequest() {
		numPendingRequests += 1;
		if (numPendingRequests == 1) {
			if (waitDialog == null) {
				waitDialog = new DialogBox(false, true);
				waitDialog.setWidget(Utils.mkEmphLabel("Please wait..."));
			}
			waitDialog.setPopupPositionAndShow(Utils.displayInCenter(waitDialog));
		}
	}
	
	protected void removePendingRequest() {
		numPendingRequests -= 1;
		if (numPendingRequests == 0) {
			waitDialog.hide();
		}
	}
}	
