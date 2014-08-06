package otgviewer.client.components;

import static otgviewer.client.components.StorageParser.packColumns;
import static otgviewer.client.components.StorageParser.packItemLists;
import static otgviewer.client.components.StorageParser.packProbes;
import static otgviewer.client.components.StorageParser.unpackColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import otgviewer.shared.ValueType;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.sample.DataColumn;
import t.viewer.shared.ItemList;

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
	
	public DataFilter chosenDataFilter; //TODO public
	public SampleClass chosenSampleClass; //TODO public
	protected String[] chosenProbes = new String[0];
	public List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	protected List<Group> chosenColumns = new ArrayList<Group>();
	protected OTGColumn chosenCustomColumn;
	public List<ItemList> chosenItemLists = new ArrayList<ItemList>(); //TODO
	
	protected final Logger logger = Utils.getLogger("dlwidget");
	private StorageParser parser;
	
	public List<Group> chosenColumns() { return this.chosenColumns; }
	
	public DataListenerWidget() {
		super();
	}
	
	public void addListener(DataViewListener l) {
		listeners.add(l);
	}
	
	//incoming signals
	@Deprecated
	public void dataFilterChanged(DataFilter filter) {		
		chosenDataFilter = filter;
		changeDataFilter(filter);
	}
	
	public void sampleClassChanged(SampleClass sc) {
		chosenSampleClass = sc;		
		changeSampleClass(sc);
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
	
	public void customColumnChanged(OTGColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		changeCustomColumn(customColumn);
	}
	
	public void itemListsChanged(List<ItemList> lists) {
		this.chosenItemLists = lists;
		changeItemLists(lists);
	}

	//outgoing signals	
	@Deprecated
	protected void changeDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
		for (DataViewListener l : listeners) {
			l.dataFilterChanged(filter);
		}		
	}

	protected void changeSampleClass(SampleClass sc) {
		chosenSampleClass = sc;		
		for (DataViewListener l : listeners) {
			l.sampleClassChanged(sc);
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
	
	protected void changeCustomColumn(OTGColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		for (DataViewListener l: listeners) {
			l.customColumnChanged(customColumn);
		}
	}
	
	protected void changeItemLists(List<ItemList> lists) {
		chosenItemLists = lists;
		for (DataViewListener l: listeners) {
			l.itemListsChanged(lists);
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
		other.itemListsChanged(chosenItemLists);
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
		return s.manager.storagePrefix();		
	}
	
	public StorageParser getParser(Screen s) {
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
//		storeDataFilter(p);
		storeColumns(p);
		storeProbes(p);
	}
	
	protected void storeColumns(StorageParser p, String key, 
			Collection<? extends OTGColumn> columns) {
		if (!columns.isEmpty()) {
			OTGColumn first = columns.iterator().next();
			logger.info("Storing columns: " + first + " : " + first.getSamples()[0] + " ...");
			p.setItem(key, packColumns(columns));
		} else {
			p.clearItem(key);
		}		
	}
	
	public void storeColumns(StorageParser p) {
		storeColumns(p, "columns", chosenColumns);
	}	
	
	protected void storeCustomColumn(StorageParser p, DataColumn<?> column) {		
		if (column != null) {
			p.setItem("customColumn", column.pack());
		} else {
			p.clearItem("customColumn");
		}		
	}

	// Separator hierarchy for columns:
	// ### > ::: > ^^^ > $$$
	protected List<Group> loadColumns(StorageParser p, DataSchema schema,
			String key,
			Collection<? extends OTGColumn> expectedColumns) throws Exception {
		//TODO unpack old format columns
		String v = p.getItem(key); // + "." + packDataFilter(chosenDataFilter));
		List<Group> r = new ArrayList<Group>();
		if (v != null && !v.equals(packColumns(expectedColumns))) {
			String[] spl = v.split("###");
			for (String cl : spl) {
				Group c = (Group) unpackColumn(schema, cl, chosenDataFilter);
				r.add(c);
			}
			return r;
		}
		return null;
	}

	public void storeProbes(StorageParser p) {
		p.setItem("probes", packProbes(chosenProbes));	
	}

	public void storeItemLists(StorageParser p) {
		p.setItem("lists", packItemLists(chosenItemLists, "###"));
	}
	
	public List<ItemList> loadItemLists(StorageParser p) {
		List<ItemList> r = new ArrayList<ItemList>();
		String v = p.getItem("lists");
		if (v != null) {
			String[] spl = v.split("###");
			for (String x: spl) {
				ItemList il = ItemList.unpack(x);
				if (il != null) {
					r.add(il);
				}
			}
		}
		return r;
	}	
	
	/**
	 * Load saved state from the local storage.
	 * If the loaded state is different from what was previously remembered in this widget, the appropriate 
	 * signals will fire.
	 */
	public void loadState(Screen sc) {
		StorageParser p = getParser(sc);		
		loadState(p, sc.schema());
	}

	public void loadState(StorageParser p, DataSchema schema) {
		SampleClass sc = new SampleClass();
		sampleClassChanged(sc);
		
		try {
			List<Group> cs = loadColumns(p, schema, 
					"columns", chosenColumns);					
			if (cs != null) {		
				logger.info("Unpacked columns: " + cs.get(0) + ": " + cs.get(0).getSamples()[0] + " ... ");
				columnsChanged(cs);
			}						
			OTGColumn cc = unpackColumn(schema, p.getItem("customColumn"), 
					chosenDataFilter);
			if (cc != null) {																		
				customColumnChanged(cc);						
			}
		} catch (Exception e) {										
			//one possible failure source is if data is stored in an incorrect format
			columnsChanged(new ArrayList<Group>());
			storeColumns(p); //overwrite the old data
			storeCustomColumn(p, null); //ditto
		}

		String v = p.getItem("probes");			
		if (v != null && !v.equals("") && !v.equals(packProbes(chosenProbes))) {
			chosenProbes = v.split("###");				
			probesChanged(chosenProbes);				
		} else if (v == null || v.equals("")) {
			probesChanged(new String[0]);
		}
		List<ItemList> lists = loadItemLists(p);
		if (lists.size() > 0) {
			chosenItemLists = lists;
			itemListsChanged(lists);
		}
	}
	
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
