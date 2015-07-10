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

package t.common.shared.sample;
import java.io.Serializable;

import javax.annotation.Nullable;

import t.common.shared.HasClass;
import t.common.shared.Packable;
import t.common.shared.SampleClass;

/**
 * A microarray sample with a unique identifier that can be represented as a string.

 * TODO make non-abstract
 */
@SuppressWarnings("serial")
abstract public class Sample implements Packable, Serializable, HasClass {

	protected SampleClass sampleClass;
	protected @Nullable String controlGroup;
	
	public Sample() {}
	
	public Sample(String _id, SampleClass _sampleClass, 
			@Nullable String controlGroup) {
		id = _id;
		sampleClass = _sampleClass;
	}
	
	private String id;	
	public String id() { return id; }
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	/**
	 * TODO change to a subclass-safe equals with canEqual etc
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Sample) {
			Sample that = (Sample) other;
			return that.canEqual(this) && id.equals(that.id());
		}
		return false;
	}
	
	protected boolean canEqual(Sample other) {
		return other instanceof Sample;
	}
	
	public SampleClass sampleClass() { return sampleClass; }
	
	abstract public String pack();
	
	@Nullable String controlGroup() { return controlGroup; }

    public String get(String parameter) {
        return sampleClass.get(parameter);
    }
    
}
