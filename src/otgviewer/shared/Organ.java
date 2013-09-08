package otgviewer.shared;

import java.io.Serializable;

import otgviewer.client.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;

/**
 * The various organs used in OTG.
 * Note that the toString names of these values must precisely 
 * match the various values in otg.Organ. 
 */
public enum Organ implements Serializable {
	Liver {
		public ImageResource image() {
			return resources.liver();
		}
	},
	Kidney {
		public ImageResource image() {
			return resources.kidney();
		}
	},
	Lung {
		public ImageResource image() {
			// TODO
			return resources.close();
		}
	},
	Spleen {
		public ImageResource image() {
			// TODO
			return resources.close();
		}
	},
	Muscle {
		public ImageResource image() {
			// TODO
			return resources.close();
		}
	};
	
	private static Resources resources = GWT.create(Resources.class);
	
	public abstract ImageResource image();
}
