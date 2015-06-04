package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import t.common.shared.clustering.ProbeClustering;
import t.viewer.shared.StringList;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class ClusteringSelector extends DataListenerWidget {

	private List<ProbeClustering> probeClusterings;

	private ClListBox algorithm;
	private ClListBox param;
	private ClListBox clustering;
	private ListChooser cluster;

	private HorizontalPanel selector;

	class ClListBox extends ListBox {
		ClListBox() {
		}

		void setItems(List<String> items) {
			String oldSel = getSelected();
			clear();

			Collections.sort(items, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (o1.length() == o2.length()) {
						return o1.compareTo(o2);
					}
					return (o1.length() < o2.length() ? -1 : 1);
				}
			});
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

		String getSelected() {
			int i = getSelectedIndex();
			if (i != -1) {
				return getItemText(i);
			} else {
				return null;
			}
		}
	}

	public ClusteringSelector() {
		makeSelector();
	}

	public void setAvailable(List<ProbeClustering> probeClusterings) {
		this.probeClusterings = probeClusterings;

		initialize();
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
				algorithmChanged(algorithm.getSelected());
			}
		});
		grid.setText(0, 0, "Algorithm");
		grid.setWidget(1, 0, algorithm);

		param = new ClListBox();
		param.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				paramChanged(param.getSelected());
			}
		});
		grid.setText(0, 1, "K");
		grid.setWidget(1, 1, param);

		clustering = new ClListBox();
		clustering.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				clusteringChanged(clustering.getSelected());
			}
		});
		grid.setText(0, 2, "Clustering");
		grid.setWidget(1, 2, clustering);

		cluster = new ListChooser(new ArrayList<StringList>(), "probes", false) {
			@Override
			protected void itemsChanged(List<String> items) {
				ClusteringSelector.this.clusterChanged(items);
			}
		};
		addListener(cluster);
		grid.setText(0, 3, "Cluster");
		grid.setWidget(1, 3, cluster);

		selector = new HorizontalPanel();
		selector.add(grid);
	}

	void initialize() {
		algorithm.setItems(new ArrayList<String>(ProbeClustering
				.collectAlgorithm(probeClusterings)));

		String a = algorithm.getSelected();
		if (a != null) {
			changeParamsFrom(new ArrayList<ProbeClustering>(
					ProbeClustering.filterByAlgorithm(probeClusterings, a)));
		}
	}

	void changeParamsFrom(List<ProbeClustering> pc) {
		param.setItems(new ArrayList<String>(ProbeClustering.collectParam1(pc)));

		String p = param.getSelected();
		if (p != null) {
			changeClusteringFrom(new ArrayList<ProbeClustering>(
					ProbeClustering.filterByParam1(pc, p)));
		}
	}

	void changeClusteringFrom(List<ProbeClustering> pc) {
		clustering.setItems(new ArrayList<String>(ProbeClustering
				.collectName(pc)));

		String c = clustering.getSelected();
		if (c != null) {
			changeClusterFrom(new ArrayList<ProbeClustering>(
					ProbeClustering.filterByName(pc, c)));
		}
	}

	void changeClusterFrom(List<ProbeClustering> pc) {
		if (pc.size() != 1) {
			throw new IllegalArgumentException(
					"Only one item is expected, but given list contains "
							+ pc.size() + " items");
		}

		cluster.setLists(pc.get(0).getClusters());
	}

	void algorithmChanged(String algorithm) {
		if (algorithm == null) {
			return;
		}
		changeParamsFrom(new ArrayList<ProbeClustering>(
				ProbeClustering.filterByAlgorithm(probeClusterings, algorithm)));
	}

	void paramChanged(String param) {
		if (param == null) {
			return;
		}

		List<ProbeClustering> filtered = new ArrayList<ProbeClustering>(
				ProbeClustering.filterByAlgorithm(probeClusterings,
						algorithm.getSelected()));

		changeClusteringFrom(new ArrayList<ProbeClustering>(
				ProbeClustering.filterByParam1(filtered, param)));
	}

	void clusteringChanged(String clustering) {
		if (clustering == null) {
			return;
		}

		List<ProbeClustering> filtered = new ArrayList<ProbeClustering>(
				ProbeClustering.filterByAlgorithm(probeClusterings,
						algorithm.getSelected()));
		filtered = new ArrayList<ProbeClustering>(
				ProbeClustering.filterByParam1(filtered, param.getSelected()));

		changeClusterFrom(new ArrayList<ProbeClustering>(
				ProbeClustering.filterByName(filtered, clustering)));
	}

	// Expected to be overridden by caller
	public abstract void clusterChanged(List<String> items);
}
