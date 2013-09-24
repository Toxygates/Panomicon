package otgviewer.shared;

import java.io.Serializable;

/**
 * The repeat types available in Toxygates.
 * Note that the toString() results of these values must precisely match the
 * values in otg.RepeatType.
 */
public enum RepeatType implements Serializable {
	Single,
	Repeat;		
}
