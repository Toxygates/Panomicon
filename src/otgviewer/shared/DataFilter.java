package otgviewer.shared;

import java.io.Serializable;

public class DataFilter implements Serializable {
	public CellType cellType;
	public Organ organ;
	public RepeatType repeatType;
	public Organism organism;
	
	public DataFilter(CellType _cellType, Organ _organ, RepeatType _repeatType, Organism _organism) {
		cellType = _cellType;
		organ = _organ;
		repeatType = _repeatType;
		organism = _organism;
	}
	
	public DataFilter() {
		
	}
}
