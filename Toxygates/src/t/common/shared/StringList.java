/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package t.common.shared;

import java.util.Arrays;
import java.util.Collection;

public class StringList extends ItemList {

	private String[] items;
	private String comment;
	
	/**
	 * This constructor is here for GWT serialization
	 */
	protected StringList() { }
	
	public StringList(String type, String name, String[] items) {
		super(type, name);
		this.items = items;
	}
	
	public Collection<String> packedItems() {
		return Arrays.asList(items);
	}
	
	public String[] items() { return items; }
	
	public int size() {
		if (items == null) {
			return 0;
		} else {
			return items.length;
		}
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
}
