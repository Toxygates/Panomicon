package otgviewer.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {

	@Source("images/human64.png")
	ImageResource human();
	
	@Source("images/rat64.png")
	ImageResource rat();
	
	@Source("images/vivo64.png")
	ImageResource vivo();
	
	@Source("images/calendar64.png")
	ImageResource calendar();
	
	@Source("images/vitro64.png")
	ImageResource vitro();
	
	@Source("images/liver32.png")
	ImageResource liver();
	
	@Source("images/kidney32.png")
	ImageResource kidney();
	
	@Source("images/chip64.png")
	ImageResource chip();
	
	@Source("images/bottle64.png")
	ImageResource bottle();
	
	@Source("images/clock64.png")
	ImageResource clock();
	
	@Source("images/16_statistics.png")
	ImageResource chart();
	
	@Source("images/16_close.png")
	ImageResource close();
	
	@Source("images/16_info.png")
	ImageResource info();
	
	@Source("images/16_search.png")
	ImageResource magnify();
	
	@Source("images/16_faq.png")
	ImageResource help();
	
	@Source("help/default.html")
	TextResource defaultHelpHTML();
	
	@Source("help/groupDefinition.png")
	ImageResource groupDefinitionHelp();
	
	@Source("help/groupDefinition.html")
	TextResource groupDefinitionHTML();	
	
	@Source("help/probeSelection.png")
	ImageResource probeSelectionHelp();
	
	@Source("help/probeSelection.html")
	TextResource probeSelectionHTML();
	
	@Source("help/dataDisplay.html")
	TextResource dataDisplayHTML();
	
	@Source("help/dataDisplay.png")
	ImageResource dataDisplayHelp();
	
	@Source("help/compoundRanking.html")
	TextResource compoundRankingHTML();
	
	@Source("help/compoundRanking.png")
	ImageResource compoundRankingHelp();
	
	@Source("help/about.html")
	TextResource aboutHTML();
	
	@Source("help/about.png")
	ImageResource about();
	
}
