package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.clustering.Clusterings;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class ClusteringSelector extends DataListenerWidget {

	protected Logger logger;
	private Screen screen;

	private ClListBox algorithm;
	private ClListBox param;
	private ClListBox clustering;
	private ListChooser cluster;

	private HorizontalPanel selector;

	private Clusterings cl = new Clusterings();

	class ClListBox extends ListBox {
		ClListBox() {
		}

		void setItems(List<String> items) {
			String oldSel = getSelected();
			clear();
			for (String i : items) {
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

	public ClusteringSelector(Screen screen) {
		this.screen = screen;
		logger = SharedUtils.getLogger("clusteringSelector");

		// algorithms = new ArrayList<Algorithm>();
		// algorithms.add(new Algorithm("Hierarchical"));

		makeSelector();

		update();
	}

	public void update() {
		// TODO implement update function using SparqlService or AppInfo
	}

	public Widget selector() {
		return selector;
	}

	private void makeSelector() {
		Grid grid = new Grid(2, 4);
		grid.setStylePrimaryName("colored");
		grid.addStyleName("slightlySpaced");

		algorithm = new ClListBox();
		algorithm.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
//				changeAlgorithm(algorithm.getSelected());
			}
		});
		algorithm.setItems(cl.getNames());
		grid.setText(0, 0, "Algorithm");
		grid.setWidget(1, 0, algorithm);

		param = new ClListBox();
		param.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
//				changeParam(param.getSelected());
			}
		});
		grid.setText(0, 1, "K");
		grid.setWidget(1, 1, param);

		clustering = new ClListBox();
		clustering.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
//				changeClustering(clustering.getSelected());
			}
		});
		grid.setText(0, 2, "Clustering");
		grid.setWidget(1, 2, clustering);
//
//		// screen.appInfo().predefinedProbeLists()
		cluster = new ListChooser(screen.appInfo().predefinedProbeLists(), "probes", false) {
			@Override
			protected void itemsChanged(List<String> items) {
				ClusteringSelector.this.itemsChanged(items);
			}
		};
		addListener(cluster);
		grid.setText(0, 3, "Cluster");
		grid.setWidget(1, 3, cluster);

		selector = new HorizontalPanel();
		selector.add(grid);
	}

//	private void changeAlgorithm(String algorithm) {
//		param.setItems(Arrays.asList(cl.lookup(algorithm).));
//	}
//
//	private void changeParam(String param) {
//		clustering.setItems(Arrays.asList(cl.lookup(algorithm.getSelected()).getAllClusterings(param)));
//	}
//
//	private void changeClustering(String param,String clustering) {
//		cluster.setItems(Arrays.asList(cl.lookup(algorithm.getSelected()).getAllClusters(param, clustering)));
//	}

	void changeFrom(int sel) {
	}

	// Override this
	public void itemsChanged(List<String> items) {
	}

}
