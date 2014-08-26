package t.common.shared.probe;

public class MultiProbe extends Probe {
	protected SimpleProbe[] probes;
	
	public MultiProbe() {}	
	public MultiProbe(String title, SimpleProbe[] probes) {
		super(title);
		this.probes = probes;
	}
	
	public SimpleProbe[] getProbes() { return probes; }
}
