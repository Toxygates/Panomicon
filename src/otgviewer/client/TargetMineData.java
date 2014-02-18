package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.client.dialog.TargetMineSyncDialog;
import otgviewer.client.rpc.MatrixService;
import otgviewer.client.rpc.MatrixServiceAsync;
import otgviewer.shared.ItemList;
import otgviewer.shared.StringList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

public class TargetMineData {

	final Screen parent;
	final MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
			.create(MatrixService.class);

	public TargetMineData(Screen parent) {
		this.parent = parent;
	}

	DialogBox dialog;
	public void importLists(final boolean asProbes) {
		final Widget ui = new TargetMineSyncDialog("Import") {
			@Override 
			protected void userProceed(String user, String pass, boolean replace) {
				doImport(user, pass, asProbes, replace);
				dialog.setVisible(false);
			}
			@Override 
			protected void userCancelled() {
				dialog.setVisible(false);
			}
		};
		dialog = Utils.displayInPopup("TargetMine import details", ui, DialogPosition.Center);
	}
	
	public void doImport(final String user, final String pass, final boolean asProbes, 
			final boolean replace) {
		matrixService.importTargetmineLists(parent.chosenDataFilter, user,
				pass, asProbes, new PendingAsyncCallback<StringList[]>(parent,
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
		final Widget ui = new TargetMineSyncDialog("Export") {
			@Override 
			protected void userProceed(String user, String pass, boolean replace) {
				doExport(user, pass, pickProbeLists(parent.chosenItemLists), replace);
				dialog.setVisible(false);
			}
			@Override 
			protected void userCancelled() {
				dialog.setVisible(false);
			}
		};
		dialog = Utils.displayInPopup("TargetMine export details", ui, DialogPosition.Center);
	}
	
	 // Could factor out code that is shared with doImport, but the two dialogs may diverge
	 // further in the future.
	public void doExport(final String user, final String pass, final List<StringList> lists, 
			final boolean replace) {
		matrixService.exportTargetmineLists(parent.chosenDataFilter, user,
				pass, lists.toArray(new StringList[0]), replace,
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
		
		for (ItemList l: into) {
			allLists.put(l.name(), l);
		}
		
		for (ItemList l: from) {
			if (replace || !allLists.containsKey(l.name())) {
				allLists.put(l.name(), l);
				addedLists++;
				addedItems += l.size();
			} else if (allLists.containsKey(l.name())) {
				nonImported += 1;
			}
		}
		
		String msg = addedLists + " lists with " + addedItems + 
				" items were successfully imported.";
		if (nonImported > 0) {
			msg = msg + "\n" + nonImported + " lists with identical names were not imported.";
		}
		Window.alert(msg);
		return new ArrayList<ItemList>(allLists.values());
	}
}
