package otgviewer.client.targetmine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.client.dialog.InteractionDialog;
import otgviewer.client.dialog.TargetMineSyncDialog;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * Client side helper for TargetMine data import/export.
 */
public class TargetMineData {

	final Screen parent;
	final TargetmineServiceAsync tmService = (TargetmineServiceAsync) GWT
			.create(TargetmineService.class);

	private Logger logger = Utils.getLogger("targetmine");
	
	public TargetMineData(Screen parent) {
		this.parent = parent;
	}

	DialogBox dialog;
	public void importLists(final boolean asProbes) {
		InteractionDialog ui = new TargetMineSyncDialog(parent, "Import") {
			@Override 
			protected void userProceed(String user, String pass, boolean replace) {
				doImport(user, pass, asProbes, replace);
				super.userProceed();
			}
			
		};
		ui.display("TargetMine import details", DialogPosition.Center);
	}
	
	public void doImport(final String user, final String pass, final boolean asProbes, 
			final boolean replace) {
		tmService.importTargetmineLists(user, pass, asProbes, 
				new PendingAsyncCallback<StringList[]>(parent,
						"Unable to import lists from TargetMine. Check your username and password. " +
						"There may also be a server error.") {
					public void handleSuccess(StringList[] data) {			
						parent.itemListsChanged(mergeLists(parent.chosenItemLists, 
								Arrays.asList(data), replace));		
						parent.storeItemLists(parent.getParser());
					}
				});
	}

	public void exportLists() {
		InteractionDialog ui = new TargetMineSyncDialog(parent, "Export") {
			@Override 
			protected void userProceed(String user, String pass, boolean replace) {
				doExport(user, pass, pickProbeLists(parent.chosenItemLists), replace);
				super.userProceed();
			}
			
		};
		ui.display("TargetMine export details", DialogPosition.Center);
	}
	
	 // Could factor out code that is shared with doImport, but the two dialogs may diverge
	 // further in the future.
	public void doExport(final String user, final String pass, final List<StringList> lists, 
			final boolean replace) {
		tmService.exportTargetmineLists(user, pass, 
				lists.toArray(new StringList[0]), replace,
				new PendingAsyncCallback<Void>(parent,
						"Unable to export lists to TargetMine. Check your username and password. " +
						"There may also be a server error.") {
					public void handleSuccess(Void v) {
						Window.alert("The lists were successfully exported.");
					}
				});
	}
	
	List<StringList> pickProbeLists(List<? extends ItemList> from) {
		List<StringList> r = new LinkedList<StringList>();
		for (ItemList l: from) {
			if (l.type().equals("probes")) {
				r.add((StringList) l);
			}
		}
		return r;
	}
	
	List<ItemList> mergeLists(List<? extends ItemList> into, List<? extends ItemList> from, boolean replace) {
		Map<String, ItemList> allLists = new HashMap<String, ItemList>();
		int addedLists = 0;
		int addedItems = 0;
		int nonImported = 0;
		int hadGenesForSpecies = 0;
		
		for (ItemList l: into) {
			allLists.put(l.name(), l);
		}
		
		for (ItemList l: from) {
			if (replace || !allLists.containsKey(l.name())) {
				allLists.put(l.name(), l);
				addedLists++;
				addedItems += l.size();
				String comment = ((StringList) l).getComment();
				Integer genesForSpecies = Integer.parseInt(comment);
				
				logger.info("Import list " + l.name() + " size " + l.size() + 
						" comment " + comment); 
				if (genesForSpecies > 0) {
					hadGenesForSpecies++;
				}
			} else if (allLists.containsKey(l.name())) {
				logger.info("Do not import list " + l.name());
				nonImported += 1;
			}
		}
		
		String msg = addedLists + " lists with " + addedItems + 
				" items were successfully imported.\nOf these, ";
		if (hadGenesForSpecies == 0) {
			msg += "no list ";
		} else {
			msg += hadGenesForSpecies + " list(s) ";
		}
		msg += " contained genes for " + parent.chosenDataFilter.organism + ".";
		
		if (nonImported > 0) {
			msg = msg + "\n" + nonImported + " lists with identical names were not imported.";
		}
		Window.alert(msg);
		return new ArrayList<ItemList>(allLists.values());
	}
}
