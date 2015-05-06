package otgviewer.shared;

import java.io.Serializable;
import java.util.Arrays;

public class Pathology implements Serializable {
	private String barcode;
	private String topography;
	private String finding;
	private boolean spontaneous;
	private String grade;
	private String viewerLink;
	
	public Pathology() { }
		
	public Pathology(String _barcode, String _topography, String _finding, 
			boolean _spontaneous, String _grade, String _viewerLink) {
		barcode = _barcode;
		topography = _topography;
		finding = _finding;
		spontaneous = _spontaneous;
		grade = _grade;
		viewerLink = _viewerLink;
	}
	
	/**
	 * Note that currently, this can sometimes be null
	 * @return
	 */
	public String barcode() {
		return barcode;
	}
	public String topography() {
		return topography;		
	}
	public String finding() {
		return finding;
	}	
	public boolean spontaneous() {
		return spontaneous;
	}
	public String grade() {
		return grade;
	}
	public String viewerLink() {
		return viewerLink;
	}
	
	@Override
	public int hashCode() {
		int r = 1;
		if (barcode != null) {
			r = r * 41 + barcode.hashCode();
		}
		if (topography != null) {
			r = r * 41 + topography.hashCode();
		}
		if (finding != null) {
			r = r * 41 + finding.hashCode();
		}
		if (grade != null) {
			r = r * 41 + grade.hashCode();
		}
		if (spontaneous) {
			r *= 41;
		}
		if (viewerLink != null) {
			r = r * 41 + viewerLink.hashCode();
		}
		return r;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Pathology) {
			Pathology op = (Pathology) other;
			Object[] th = new Object[] { barcode, topography, finding, 
					spontaneous, grade, viewerLink };
			Object[] oth = new Object[] { op.barcode(), op.topography(), op.finding(), 
					op.spontaneous(), op.grade(), op.viewerLink()} ;
			return Arrays.deepEquals(th, oth);			
		}
		return false;
	}
	
}
