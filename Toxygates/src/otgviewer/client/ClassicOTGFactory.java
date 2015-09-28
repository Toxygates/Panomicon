package otgviewer.client;

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.FullCompoundRanker;

/**
 * This factory lets the UI mimic the "classic" Toxygates interface
 * as released in 2013.
 */
public class ClassicOTGFactory extends OTGFactory {
  @Override
  public CompoundRanker compoundRanker(Screen _screen, CompoundSelector selector) {
    return new FullCompoundRanker(_screen, selector);
  }

  @Override
  public GeneSetEditor geneSetEditor(Screen screen) {
    return new GeneSetEditor(screen) {
      @Override
      protected boolean hasClustering() {
        return false;
      }
    };
  }

  @Override
  public boolean hasHeatMapMenu() {
    return false;
  }
}
