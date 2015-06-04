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

package otgviewer.shared;

import java.io.Serializable;

/**
 * Ranking rules for compounds. Also see RuleType.
 * @author johan
 *
 */
public class RankRule implements Serializable {
	
	public RankRule() { }
	public RankRule(RuleType type, String probe) {
		_probe = probe;
		_type = type;		
	}
	
	private RuleType _type;
	public RuleType type() { return _type; }
	public void setRuleType(RuleType type) { _type = type; }
	
	private String _probe;
	public String probe() { return _probe; }
	public void setProbe(String probe) { _probe = probe; }	
	
	//Used by reference compound rule only
	private String _compound;
	public String compound() { return _compound; }	
	public void setCompound(String compound) { _compound = compound; }
	
	//Used by synthetic rule only
	private double[] _data;
	public double[] data() { return _data; }
	public void setData(double[] data) { _data = data; }
	
	private String _dose;
	public String dose() { return _dose; }	
	public void setDose(String dose) { _dose = dose; }
	
}
