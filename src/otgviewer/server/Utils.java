package otgviewer.server;

import otg.Species;
import otgviewer.shared.DataFilter;

public class Utils {
	static Species speciesFromFilter(DataFilter filter) {
		switch(filter.organism) {
			case Human:
				return new otg.Human();
			case Rat:
				return new otg.Rat();				
		}
		return null;
	}
	
}
