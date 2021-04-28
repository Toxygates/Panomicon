package t.viewer.client.screen.data;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import t.clustering.shared.Algorithm;
import t.clustering.shared.ClusteringList;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.InputDialog;
import t.viewer.client.screen.ImportingScreen;
import t.viewer.shared.StringList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClusterSaveDialog {

    /**
     * Display a name entry dialog that saves a list of clusters.
     * @param screen Parent screen, capable of storing clusters
     * @param lists Probe clusters
     * @param nameSuffixes Optional suffix names that will be added to each cluster
     * @param algorithm Optional algorithm that was used to generate the clusters
     * @param caption
     * @param message
     */
    public static void saveAction(
            final ImportingScreen screen, final List<Collection<String>> lists,
            final @Nullable List<String> nameSuffixes, final @Nullable Algorithm algorithm,
                            String caption, String message) {
        final String type = ClusteringList.USER_CLUSTERING_TYPE;

        final DialogBox db = new DialogBox(true, true);
        InputDialog entry = new InputDialog(message) {
            @Override
            protected void onChange(String name) {
                if (name == null) { // on Cancel clicked
                    db.setVisible(false);
                    return;
                }

                if (!screen.clusteringLists().validateNewObjectName(name, false)) {
                    return;
                }

                List<String> names = generateNameList(name, lists.size(), nameSuffixes);
                List<StringList> clusters = new ArrayList<StringList>();
                for (int i = 0; i < lists.size(); ++i) {
                    clusters.add(new StringList(StringList.PROBES_LIST_TYPE,
                            names.get(i), lists.get(i).toArray(new String[0])));
                }

                ClusteringList cl =
                        new ClusteringList(type, name, algorithm, clusters.toArray(new StringList[0]));

                screen.clusteringLists().put(name, cl);
                screen.clusteringListsChanged();
                db.setVisible(false);

                Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_SAVE_CLUSTERS);
                Window.alert("Clusters are successfully saved.");
            }
        };

        Utils.displayInPopup(db, caption, entry, false, DialogPosition.Center, null);
    }

    private static List<String> generateNameList(String base, int size, @Nullable List<String> nameSuffixes) {
        if (size > 1) {
            return getSerialNumberedNames(base, size, nameSuffixes);
        } else {
            return new ArrayList<String>(Arrays.asList(new String[] {base}));
        }
    }

    private static List<String> getSerialNumberedNames(String base, int count, @Nullable List<String> nameSuffixes) {
        List<String> names = new ArrayList<String>(count);
        for (int i = 0; i < count; ++i) {
            String name = base + " " + (i + 1);
            if (nameSuffixes != null) {
                name = name + " (" + nameSuffixes.get(i) + ")";
            }
            names.add(name);
        }
        return names;
    }

}
