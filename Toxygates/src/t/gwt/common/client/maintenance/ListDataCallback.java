/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.common.client.maintenance;

import java.util.Arrays;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ListDataProvider;

public class ListDataCallback<T> implements AsyncCallback<T[]> {

	final ListDataProvider<T> provider;
	final String description;
	
	public ListDataCallback(ListDataProvider<T> provider, String description) {
		this.provider = provider;
		this.description = description;
	}

	@Override
	public void onFailure(Throwable caught) {
		Window.alert("Unable to obtain " + description + " from server.");
		
	}

	@Override
	public void onSuccess(T[] result) {
		provider.setList(Arrays.asList(result));		
	}

}
