package otgviewer.shared;

import java.io.Serializable;

/**
 * The various organs used in OTG.
 * Note that the toString names of these values must precisely 
 * match the various values in otg.Organ. 
 */
public enum Organ implements Serializable {
	Liver,
	Kidney,
	Lung,
	Spleen,
	Muscle,
	LymphNode;
}
	
