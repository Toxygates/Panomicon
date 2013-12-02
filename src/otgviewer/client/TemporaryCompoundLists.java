package otgviewer.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemporaryCompoundLists {

	/**
	 * Temporary hardcoded lists for testing. Will eventually be stored as RDF.
	 * @return
	 */
	static Map<String, List<String>> predefinedLists() {
		Map<String, List<String>> r = new HashMap<String, List<String>>();
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
		r.put("Negative elevation bilirubin", Arrays.asList(negElBilib));
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
		r.put("Glutathione depletion", Arrays.asList(glutDepl));
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
		r.put("Peroxisome prolifeator", Arrays.asList(peroxiProl));
		
		return r;
	}	
}
