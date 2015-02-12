package t.common.shared;

import java.util.Date;

import javax.annotation.Nullable;

/**
 * Meta-records such as batches, instances, datasets etc.
 */
public interface DataRecord {
	@Nullable
	public Date getDate();
	
	public String getComment();
	
	public String getTitle();
	
	/**
	 * User-readable title string
	 * @return
	 */
	public String getUserTitle();
}
