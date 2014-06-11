package t.admin.server;

import gwtupload.server.UploadServlet;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.fileupload.FileItem;

import t.admin.client.MaintenanceService;
import t.admin.shared.AddBatchResult;
import t.admin.shared.AddPlatformResult;
import t.admin.shared.Progress;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MaintenanceServiceImpl extends RemoteServiceServlet implements MaintenanceService {

	public AddBatchResult tryAddBatch() {				
		List<FileItem> items = UploadServlet.getSessionFileItems(getThreadLocalRequest());
		if (items != null) {
			for (FileItem fi : items) {
				System.out.println("File " + fi.getName() + " size "
						+ fi.getSize() + " field: " + fi.getFieldName());
			}
		}
		return null;
	}

	public AddPlatformResult tryAddPlatform() {
		return null;
	}

	public boolean tryDeleteBatch(String id) {
		return false;
	}

	public boolean tryDeletePlatform(String id) {
		return false;
	} 
	
	public void cancelTask() {
		
	}

	public Progress getProgress() {
		return new Progress("Nothing", 0);
	}

	/**
	 * Retrive the last uploaded file with a particular tag.
	 * @param tag the tag to look for.
	 * @return
	 */
	@Nullable
	private FileItem getFile(String tag) {
		FileItem last = null;
		List<FileItem> items = UploadServlet.getSessionFileItems(getThreadLocalRequest());
		for(FileItem fi: items) {
			if (fi.getFieldName().startsWith(tag)) {
				last = fi;
			}			
		}
		return last;
	}
}
