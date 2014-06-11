package t.admin.server;

import gwtupload.server.UploadServlet;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload.FileItem;

import t.admin.client.MaintenanceService;
import t.admin.shared.AddBatchResult;
import t.admin.shared.AddPlatformResult;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MaintenanceServiceImpl extends RemoteServiceServlet implements MaintenanceService {

	public AddBatchResult tryAddBatch() {		
		
		List<FileItem> items = UploadServlet.getSessionFileItems(getThreadLocalRequest());
		for(FileItem fi: items) {
			System.out.println("File " + fi.getName() + " size " + 
					fi.getSize() + " field: " + fi.getFieldName());
			Iterator i = fi.getHeaders().getHeaderNames();
			while (i.hasNext()) {
				System.out.println("Header " + i.next());
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

}
