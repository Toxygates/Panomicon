package otgviewer.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {

	@Source("human64.png")
	ImageResource human();
	
	@Source("rat64.png")
	ImageResource rat();
	
	@Source("vivo64.png")
	ImageResource vivo();
	
	@Source("vitro64.png")
	ImageResource vitro();
	
	@Source("liver64.png")
	ImageResource liver();
	
	@Source("kidney64.png")
	ImageResource kidney();
	
	@Source("chip64.png")
	ImageResource chip();
	
	@Source("bottle64.png")
	ImageResource bottle();
}
