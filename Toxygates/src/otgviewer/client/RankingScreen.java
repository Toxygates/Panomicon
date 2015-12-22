package otgviewer.client;

import static t.common.client.Utils.makeScrolled;
import otgviewer.client.components.FilterTools;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.ranking.CompoundRanker;
import t.common.shared.SampleClass;

import com.google.gwt.user.client.ui.Widget;

public class RankingScreen extends Screen {

  public static final String key = "rank";
  
  private RankingCompoundSelector cs;
  private FilterTools filterTools;
  
  public RankingScreen(ScreenManager man) {
    super("Compound ranking", key, false, man,
        resources.defaultHelpHTML(), null);
    
    filterTools = new FilterTools(this);
    
    String majorParam = man.schema().majorParameter();
    cs = new RankingCompoundSelector(this, man.schema().title(majorParam));
    this.addListener(cs);
    cs.setStylePrimaryName("compoundSelector");
  }
  
  @Override
  protected void addToolbars() {
      super.addToolbars();
      addToolbar(filterTools, 45);
      addLeftbar(cs, 350);
  }
  
  public Widget content() {
    CompoundRanker cr = factory().compoundRanker(this, cs);
    return makeScrolled(cr);    
  }
  
  @Override
  public void changeSampleClass(SampleClass sc) {
      //On this screen, ignore the blank sample class set by
      //DataListenerWidget
      if (!sc.getMap().isEmpty()) {
          super.changeSampleClass(sc);
      }
  }
  
  @Override
  public void resizeInterface() {
      //Test carefully in IE8, IE9 and all other browsers if changing this method
      cs.resizeInterface();
      super.resizeInterface();        
  }
  
  public void show() {
    super.show();
  }

}
