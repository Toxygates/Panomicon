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
	
	public boolean equals(Object other) {
		if (other instanceof DataFilter) {
			DataFilter dfo = (DataFilter) other;
			return (dfo.cellType == cellType && dfo.organ == organ &&
					dfo.repeatType == repeatType && dfo.organism == organism);
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return cellType.hashCode() + 41 * (organ.hashCode() + 
				41 * (repeatType.hashCode() + 41 * organism.hashCode()));
	}
	
	public static DataFilter unpack(String s) {
		String[] parts = s.split(",");
		assert(parts.length == 4);
		
		try {
			DataFilter r = new DataFilter(CellType.valueOf(parts[0]),
					Organ.valueOf(parts[1]), RepeatType.valueOf(parts[2]),
					Organism.valueOf(parts[3]));			
			return r;
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
