package t.admin.shared;

import java.util.Date;

import javax.annotation.Nullable;

public interface DataRecord {
	@Nullable
	public Date getDate();
	
	public String getComment();
	
	public String getTitle();
}
