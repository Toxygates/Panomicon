package bioweb.shared.array;

import java.io.Serializable;
import java.util.Arrays;
import bioweb.shared.SharedUtils;

/**
 * A way of grouping microarray samples.
 * Unique colors for each group can be generated.
 * @author johan
 *
 * @param <S>
 */
public class SampleGroup<S extends Sample> implements DataColumn<S>, Serializable, Comparable<SampleGroup> {

	/**
	 * This list was generated using the service at
	 * http://tools.medialab.sciences-po.fr/iwanthue/
	 */
	protected static final String[] groupColors = new String[] { "#97BDBD", "#C46839", "#9F6AC8", "#9CD05B", 
		"#513C4D", "#6B7644", "#C75880" };
	private static int nextColor = 0;
	
	
	public SampleGroup() { }
	
	protected String name;
	protected String color;
	protected S[] _samples;
	
	public SampleGroup(String name, S[] samples, String color) {
		this.name = name;
		this._samples = samples;
		this.color = color;
	}
	
	public SampleGroup(String name, S[] samples) {
		this(name, samples, pickColor());
	}
	
	private static synchronized String pickColor() {
		nextColor += 1;
		if (nextColor > groupColors.length) {
			nextColor = 1;
		}
		return groupColors[nextColor - 1];
	}
	
	public S[] samples() { return _samples; }
	public S[] getSamples() { return _samples; }
	
	public String getName() { return name; }
	public String getShortTitle() { return name; }
	
	public String toString() { return name; }

	public String getColor() { return color; }
		
	public String getStyleName() { return "Group" + getColorIndex(); }
	
	private int getColorIndex() {
		return SharedUtils.indexOf(groupColors, color);
	}
	
	public String pack() {
		StringBuilder s = new StringBuilder();
		s.append("Group:::");
		s.append(name + ":::"); //!!
		s.append(color + ":::");
		for (S b : _samples) {
			s.append(b.pack());
			s.append("^^^");
		}
		return s.toString();
	}
	
	
	@Override
	public int compareTo(SampleGroup other) {
		return name.compareTo(other.getName());
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(_samples) * 41 + name.hashCode();
	}	

	@Override
	public boolean equals(Object other) {
		if (other instanceof SampleGroup) {
			SampleGroup<?> that = (SampleGroup<?>) other;
			return that.canEqual(this) && Arrays.deepEquals(this._samples, that.samples()) && name.equals(that.getName())
					&& color.equals(that.getColor());
		}
		return false;
	}
	
	protected boolean canEqual(Object other) {
		return other instanceof SampleGroup;
	}
	
}
