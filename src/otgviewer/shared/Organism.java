package otgviewer.shared;

import java.io.Serializable;

import otgviewer.client.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;

/**
 * The organisms available in Toxygates.
 * The toString() results of these values must precisely match the values in otg.Species. 
 */
public enum Organism implements Serializable {
	Human {
		public ImageResource image() {
			return resources.human();
		}
	},
	Rat {
		public ImageResource image() {
			return resources.rat();
		}
	};
	
	private static Resources resources = GWT.create(Resources.class);
	
	public abstract ImageResource image();
}
