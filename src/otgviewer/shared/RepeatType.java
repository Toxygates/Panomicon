package otgviewer.shared;

import java.io.Serializable;

import otgviewer.client.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;

/**
 * The repeat types available in Toxygates.
 * Note that the toString() results of these values must precisely match the
 * values in otg.RepeatType.
 */
public enum RepeatType implements Serializable {
	Single {
		public ImageResource image() {
			return resources.clock();
		}
	},
	Repeat {
		public ImageResource image() {
			return resources.calendar();
		}
	};
	
	private static Resources resources = GWT.create(Resources.class);
	
	public abstract ImageResource image();
}
