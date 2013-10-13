package otgviewer.shared;

import java.io.Serializable;

/**
 * The organisms available in Toxygates.
 * The toString() results of these values must precisely match the values in otg.Species. 
 */
public enum Organism implements Serializable {
	Human,
	Rat;
}
