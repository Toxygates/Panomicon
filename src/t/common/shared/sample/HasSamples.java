package t.common.shared.sample;

import javax.annotation.Nullable;

public interface HasSamples<S extends Sample> {

	@Nullable
	public S[] getSamples();
}
