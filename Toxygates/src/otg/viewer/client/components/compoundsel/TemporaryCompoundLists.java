/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otg.viewer.client.components.compoundsel;

import static t.viewer.shared.StringList.COMPOUND_LIST_TYPE;

import java.util.ArrayList;
import java.util.List;

import t.viewer.shared.StringList;

public class TemporaryCompoundLists {

	/*
	 * TODO Temporary hardcoded lists for testing. Will eventually be stored as RDF.
	 */
	static List<StringList> predefinedLists() {
		List<StringList> r = new ArrayList<StringList>();
		String[] negElBilib = new String[] { 
				"chloramphenicol",
				"aspirin",
				"indomethacin",
				"tetracycline",
				"acetaminophen",
				"allopurinol",
				"chlorpromazine",
				"isoniazid",
				"methyltestosterone",
				"promethazine",
				"ethionamide",
				"gentamicin",
				"iproniazid",
				"metformin",
				"coumarin",
				"diltiazem",
				"hydroxyzine",
				"tamoxifen",
				"ticlopidine",
				"carbon_tetrachloride",
				"allyl_alcohol",
				"perhexiline"		
		};
		r.add(new StringList(COMPOUND_LIST_TYPE, "Negative elevation bilirubin", 
				negElBilib)
		);
				
		String[] glutDepl = new String[] {
				"aspirin",
				"acetaminophen",
				"chlorpromazine",
				"clofibrate",
				"glibenclamide",
				"phenylbutazone",
				"coumarin",
				"carbon_tetrachloride",
				"thioacetamide",
				"hexachlorobenzene",
				"methapyrilene",
				"bromobenzene"
		};
		r.add(new StringList(COMPOUND_LIST_TYPE, "Glutathione depletion",
				glutDepl)
		);
				
		String[] peroxiProl = new String[] {
				"aspirin",
				"indomethacin",
				"rifampicin",
				"acetaminophen",
				"allopurinol",
				"carbamazepine",
				"chlorpromazine",
				"clofibrate",
				"diazepam",
				"gemfibrozil",
				"isoniazid",
				"nitrofurantoin",
				"phenobarbital",
				"phenylbutazone",
				"phenytoin",
				"propylthiouracil",
				"benzbromarone",
				"coumarin",
				"cyclophosphamide",
				"omeprazole",
				"carbon_tetrachloride",
				"allyl_alcohol",
				"thioacetamide",
				"ethionine",
				"hexachlorobenzene",
				"bromobenzene"
		};
		r.add(new StringList(COMPOUND_LIST_TYPE, "Peroxisome proliferator",
				peroxiProl)
		);
		
		return r;
	}	
}
