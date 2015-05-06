package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import t.viewer.shared.StringList;

public class TemporaryCompoundLists {

	/*
	 * TODO Temporary hardcoded lists for testing. Will eventually be stored as RDF.
	 * @return
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
		r.add(new StringList("compounds", "Negative elevation bilirubin", 
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
		r.add(new StringList("compounds", "Glutathione depletion",
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
		r.add(new StringList("compounds", "Peroxisome proliferator",
				peroxiProl)
		);
		
		return r;
	}	
}
