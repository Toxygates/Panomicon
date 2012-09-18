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
	
	public static DataFilter unpack(String s) {
		String[] parts = s.split(",");
		assert(parts.length == 4);
		
		try {
			return new DataFilter(CellType.valueOf(parts[0]),
					Organ.valueOf(parts[1]), RepeatType.valueOf(parts[2]),
					Organism.valueOf(parts[3]));
		} catch (Exception e) {
			return null;
		}
	}
	
	public String pack() {
		return cellType.name() + "," + organ.name() + "," + repeatType.name() + "," + organism.name();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(organism.name());
		s.append(", ");
		s.append(cellType.name());
		s.append(", ");
		switch (cellType) {
		case Vivo:
			s.append(organ.name());
			s.append(", ");
		case Vitro:
			break;
		}
		s.append(repeatType.name());		
		return s.toString();
	}
}
