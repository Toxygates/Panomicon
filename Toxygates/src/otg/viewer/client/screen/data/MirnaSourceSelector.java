package otg.viewer.client.screen.data;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import t.common.shared.Pair;
import t.viewer.client.Utils;
import t.viewer.shared.mirna.MirnaSource;

/**
 * A dialog for selecting among a set of available miRNA sources and,
 * optionally, setting thresholds and selecting a source combination method.
 */
public class MirnaSourceSelector extends Composite {
  MirnaSource[] availableSources;
  Grid grid;
  
  Map<MirnaSource, CheckBox> active = new HashMap<MirnaSource, CheckBox>();
  Map<MirnaSource, TextBox> limits = new HashMap<MirnaSource, TextBox>();
  Map<MirnaSource, ListBox> levelSelectors = new HashMap<MirnaSource, ListBox>();
  
  /**
   * @param preferredSources Sources that are already selected. Only IDs and
   * cutoff values will be respected.
   */
  public MirnaSourceSelector(MirnaSource[] availableSources,
                             @Nullable List<MirnaSource> preferredSources) {
    grid = new Grid(availableSources.length + 1, 6);
    initWidget(grid);
    grid.setCellSpacing(5);
    this.availableSources = availableSources;
    
    grid.setWidget(0, 0, Utils.mkEmphLabel("Active"));
    grid.setWidget(0, 1, Utils.mkEmphLabel("Title"));
    grid.setWidget(0, 2, Utils.mkEmphLabel("Comment"));
    grid.setWidget(0, 3, Utils.mkEmphLabel("Score cutoff"));
    grid.setWidget(0, 4, Utils.mkEmphLabel("Interactions"));
    grid.setWidget(0, 5, Utils.mkEmphLabel("Information"));
    
    int row = 1;
    for (MirnaSource m: availableSources) {
      addSource(m, row);
      row++;
    }
    
    if (preferredSources != null) {
      Map<String, Double> preferred = 
          preferredSources.stream().collect(Collectors.toMap(ms -> ms.id(), ms -> ms.limit()));
      for (MirnaSource s: availableSources) {
        if (preferred.containsKey(s.id())) {
          active.get(s).setValue(true);
          Double preferredLimit = preferred.get(s.id());
          if (s.scoreLevels() == null) {
            limits.get(s).setValue("" + preferredLimit);
          } else {
            int selIdx = 0;
            for (Pair<String, Double> l: s.scoreLevels()) {
              if (l.second() == preferredLimit) {
                break;
              }
              selIdx++;
            }                           
            levelSelectors.get(s).setSelectedIndex(selIdx);            
          }
        }
      }
    }    
  }
  
  protected void addSource(MirnaSource source, int row) {
    CheckBox state = new CheckBox("");
    grid.setWidget(row, 0, state);
    active.put(source, state);
    
    Label title = new Label(source.title());
    grid.setWidget(row, 1, title);
    
    Label comment = new Label(source.comment());
    grid.setWidget(row, 2, comment);
    
    List<? extends Pair<String,Double>> levels = source.scoreLevels();
    //If the source has levels, we offer a discrete selection of values.
    if (levels != null) {
      ListBox limitInput = new ListBox();
      for (Pair<String, Double> level: levels) {
        limitInput.addItem(level.first());
      }
      levelSelectors.put(source, limitInput);
      grid.setWidget(row, 3, limitInput);
    } else if (source.hasScores()) {
      TextBox limitInput = new TextBox();
      limitInput.setText("" + source.limit());
      limits.put(source, limitInput);
      grid.setWidget(row, 3, limitInput);      
    }
    
    Label size = new Label("" + source.size());
    grid.setWidget(row, 4, size);
    
    String url = source.infoURL();
    if (url != null) {
      grid.setWidget(row, 5, Utils.linkTo(url, "Official page"));
    }
    
  }
  
  public Collection<MirnaSource> getSelection() throws NumberFormatException {
    List<MirnaSource> result = Arrays.stream(availableSources).filter(s -> active.get(s).getValue()).
      collect(Collectors.toList());
    
    for (MirnaSource source: result) {
      if (source.scoreLevels() != null) {
        String selectedLevel = levelSelectors.get(source).getSelectedItemText();
        Double selectedValue = source.scoreLevelMap().get(selectedLevel);
        source.setLimit(selectedValue);
      } else if (source.hasScores()) {
        String selectedLimit = limits.get(source).getValue();        
        source.setLimit(Double.valueOf(selectedLimit));
      }
    }
        
    return result;
  }
}
