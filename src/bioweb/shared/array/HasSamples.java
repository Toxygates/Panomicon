package bioweb.shared.array;

import javax.annotation.Nullable;

public interface HasSamples<S extends Sample> {

	@Nullable
	public S[] getSamples();
}
