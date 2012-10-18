package otgviewer.shared;

import java.io.Serializable;

public class Pathology implements Serializable {
	private String _barcode;
	private String _topography;
	private String _finding;
	private boolean _spontaneous;
	private String _grade;
	
	public Pathology() { }
		
	public Pathology(String barcode, String topography, String finding, boolean spontaneous, String grade) {
		_barcode = barcode;
		_topography = topography;
		_finding = finding;
		_spontaneous = spontaneous;
		_grade = grade;
	}
	
	/**
	 * Note that currently, this can sometimes be null
	 * @return
	 */
	public String barcode() {
		return _barcode;
	}
	public String topography() {
		return _topography;		
	}
	public String finding() {
		return _finding;
	}	
	public boolean spontaneous() {
		return _spontaneous;
	}
	public String grade() {
		return _grade;
	}
	
}
