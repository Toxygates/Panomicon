package otgviewer.shared;

import java.io.Serializable;


/**
 * A set of constraints for filtering OTG samples.
 * TODO: This is deprecated, to be removed
 * @author johan
 */
@Deprecated
public class DataFilter implements Serializable {
	public CellType cellType;
	public Organ organ;
	public RepeatType repeatType;
	public Organism organism;

	public DataFilter() { }
	
	public DataFilter(DataFilter copy) {
		cellType = copy.cellType;
		organ = copy.organ;
		repeatType = copy.repeatType;
		organism = copy.organism;
	}
	
	public DataFilter(CellType _cellType, Organ _organ, RepeatType _repeatType, Organism _organism) {
		cellType = _cellType;
		organ = _organ;
		repeatType = _repeatType;
		organism = _organism;
	}
	
	public boolean permits(Barcode b) {
		BUnit u = b.getUnit();
		if (organ != null && organ != u.getOrgan()) {
			return false;
		}
		if (organism != null && organism != u.getOrganism()) {
			return false;
		}
		if (cellType != null && cellType != u.getCellType()) {
			return false;
		}
		if (repeatType != null && repeatType != u.getRepeatType()) {
			return false;
		}
		return true;
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
