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

/**
 * A Pair whose equality and hashCode only depend on the first member.
 * @author johan
 *
 * @param <T>
 * @param <U>
 */
public class FirstKeyedPair<T, U> extends Pair<T, U> {

	//GWT serialization constructor
	public FirstKeyedPair() { }
	
	public FirstKeyedPair(T t, U u) {
		super(t, u);
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other instanceof FirstKeyedPair) {
			return _t.equals(((FirstKeyedPair<T, U>) other).first());
		} else {
			return false;
		}
	}
	
	@Override 
	public int hashCode() {
		return _t.hashCode();
	}

}
