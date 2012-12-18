package otgviewer.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {

	@Source("images/human64.png")
	ImageResource human();
	
	@Source("images/rat64.png")
	ImageResource rat();
	
	@Source("images/vivo64.png")
	ImageResource vivo();
	
	@Source("images/vitro64.png")
	ImageResource vitro();
	
	@Source("images/liver64.png")
	ImageResource liver();
	
	@Source("images/kidney64.png")
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
	
}
