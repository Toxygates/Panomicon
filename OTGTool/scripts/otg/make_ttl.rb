#! /usr/bin/env ruby

def quote_by_backslash(str)
   str.gsub(/['"`;\\]/){|q| '\\' + q}
end

class Numeric
  def round_n(nth)
    num=self*(10**(-nth))
    return num.round()*(10**nth)
  end
end

class Array
  def combination(num)
    return [] if num < 1 || num > size
    return map{|e| [e] } if num == 1
    tmp = self.dup
    self[0, size - (num - 1)].inject([]) do |ret, e|
      tmp.shift
      ret += tmp.combination(num - 1).map{|a| a.unshift(e) }
    end
  end

  def product(other)
    inject([]) do |ret, es|
      ret += other.map{|eo| [es, eo]}
    end
  end
end



n=0;n_total=0
File.read("/the/location/of/attribute/files").each do |line|
n+=1
  if n == 1
    header_metaline=line.chomp.split(/\t/)[0..30]
#    p line.chomp.split(/\t/).size
#    p header_metaline
    next
  end
  sample_id          = line.chomp.split(/\t/)[0]
  bioassay_id        = line.chomp.split(/\t/)[1]
  original_code      = line.chomp.split(/\t/)[2]
  barcode_orig       = line.chomp.split(/\t/)[3]
  barcode            = line.chomp.split(/\t/)[4]
  arr_design         = line.chomp.split(/\t/)[5]
  exp_id             = line.chomp.split(/\t/)[6]
  group_id           = line.chomp.split(/\t/)[7]
  tgp_exp_id         = line.chomp.split(/\t/)[8]
  individual_id      = line.chomp.split(/\t/)[9]
  animal_no          = line.chomp.split(/\t/)[10]
  organ_id           = line.chomp.split(/\t/)[11]
  material_id        = line.chomp.split(/\t/)[12]
  compound_name      = line.chomp.split(/\t/)[13]
  compound_abbr      = line.chomp.split(/\t/)[14]
  compound_no        = line.chomp.split(/\t/)[15]
  product_information= line.chomp.split(/\t/)[16]
  cro_type           = line.chomp.split(/\t/)[17]
  species            = line.chomp.split(/\t/)[18]
  test_type          = line.chomp.split(/\t/)[19]
  sin_rep_type       = line.chomp.split(/\t/)[20]
  sex_type           = line.chomp.split(/\t/)[21]
  strain_type        = line.chomp.split(/\t/)[22]
  adm_route_type     = line.chomp.split(/\t/)[23]
  animal_age         = line.chomp.split(/\t/)[24]
  dose               = line.chomp.split(/\t/)[25]
  vehicle_dose       = line.chomp.split(/\t/)[26]
  medium_type        = line.chomp.split(/\t/)[27]
  dose_unit          = line.chomp.split(/\t/)[28]
  dose_level         = line.chomp.split(/\t/)[29]
  sacri_period       = line.chomp.split(/\t/)[30]
  del_flag           = line.chomp.split(/\t/)[31]

  fixed_uri = "http://127.0.0.1:3333"


  print "\n<#{fixed_uri}/#{sample_id}> a \"tgp_uniq_id\" ;\"\n"
  print "\trdf:Alt \"#{barcode}\" ;\n"
  print "\t<#{fixed_uri}/bioassay_id> \"#{bioassay_id}\";\n"
  print "\t<#{fixed_uri}/original_code> \"#{original_code}\" ;\n"
  print "\t<#{fixed_uri}/barcode_orig> \"#{barcode_orig}\" ;\n"
  print "\t<#{fixed_uri}/arr_design> \"#{arr_design}\" ;\n"         
  print "\t<#{fixed_uri}/exp_id> \"#{exp_id}\" ;\n"             
  print "\t<#{fixed_uri}/group_id> \"#{group_id}\" ;\n"           
  print "\t<#{fixed_uri}/tgp_exp_id> \"#{tgp_exp_id}\" ;\n"         
  print "\t<#{fixed_uri}/individual_id> \"#{individual_id}\" ;\n"      
  print "\t<#{fixed_uri}/animal_no> \"#{animal_no}\" ;\n"          
  print "\t<#{fixed_uri}/organ_id> \"#{organ_id}\" ;\n"           
  print "\t<#{fixed_uri}/material_id> \"#{material_id}\" ;\n"        
  print "\t<#{fixed_uri}/compound_name> \"#{compound_name}\" ;\n"      
  print "\t<#{fixed_uri}/compound_abbr> \"#{compound_abbr}\" ;\n"      
  print "\t<#{fixed_uri}/compound_no> \"#{compound_no}\" ;\n"        
  print "\t<#{fixed_uri}/product_information> \"#{product_information}\" ;\n"
  print "\t<#{fixed_uri}/cro_type> \"#{cro_type}\" ;\n"           
  print "\t<#{fixed_uri}/species> \"#{species}\" ;\n"            
  print "\t<#{fixed_uri}/test_type> \"#{test_type}\" ;\n"          
  print "\t<#{fixed_uri}/sin_rep_type> \"#{sin_rep_type}\" ;\n"       
  print "\t<#{fixed_uri}/sex_type> \"#{sex_type}\" ;\n"           
  print "\t<#{fixed_uri}/strain_type> \"#{strain_type}\" ;\n"        
  print "\t<#{fixed_uri}/adm_route_type> \"#{adm_route_type}\" ;\n"     
  print "\t<#{fixed_uri}/animal_age> \"#{animal_age}\" ;\n"         
  print "\t<#{fixed_uri}/dose> \"#{dose}\" ;\n"               
  print "\t<#{fixed_uri}/vehicle_dose> \"#{vehicle_dose}\" ;\n"       
  print "\t<#{fixed_uri}/medium_type> \"#{medium_type}\" ;\n"        
  print "\t<#{fixed_uri}/dose_unit> \"#{dose_unit}\" ;\n"          
  print "\t<#{fixed_uri}/dose_level> \"#{dose_level}\" ;\n"         
  print "\t<#{fixed_uri}/sacri_period> \"#{sacri_period}\" ;\n"
  print "\t<#{fixed_uri}/del_flag> \"#{del_flag}\" .\n"           

end



if false

  biochem_line = line.chomp.split(/\t/)[22..68] ##TERMINAL_BW(g),,,
#  meta_line = line.chomp.split(/\t/)[2..21] ##,,,DOSE_LEVEL  ## [0]:sample_id, [1]:barcode, [8]:compound_name, 

  barcode       = line.chomp.split(/\t/)[1]
  arr_design    = line.chomp.split(/\t/)[2]
  exp_id        = line.chomp.split(/\t/)[3]
  group_id      = line.chomp.split(/\t/)[4]
  individual_id = line.chomp.split(/\t/)[5]
  organ_id      = line.chomp.split(/\t/)[6]
  material_id   = line.chomp.split(/\t/)[7]
  cpd_abbr      = line.chomp.split(/\t/)[9]
  cpd_no        = line.chomp.split(/\t/)[10]

#### 1st information
  header1 = "#{arr_design}\t#{exp_id}\t#{group_id}\t#{individual_id}\t#{organ_id}\t#{material_id}\t#{cpd_name}\t#{cpd_abbr}\t#{cpd_no}"
  meta_line = line.chomp.split(/\t/)[11..21] # species 2 dose_level
  casnum = name2cas[cpd_name]
  keggdr = name2kgd[cpd_name]
  keggcp = name2kgc[cpd_name]
  # original print "#{sample_id}\ttgp_uniq_id\t#{barcode}\t#{header1}\t#{casnum}\t#{keggdr}\t#{keggcp}\t#{meta_line.join("\t")}\t#{biochem_line.join("\t")}\t"
  fixed_uri = "http://127.0.0.1:3333"


  print "\n<#{fixed_uri}/#{sample_id}> a \"tgp_uniq_id\" ;\"\n"
  print "\trdf:Alt \"#{barcode}\" ;\n"
  print "\t<#{fixed_uri}/arr_design> \"#{arr_design}\" ;\n"
  print "\t<#{fixed_uri}/exp_id> \"#{exp_id}\" ;\n"
  print "\t<#{fixed_uri}/group_id> \"#{group_id}\" ;\n"
  print "\t<#{fixed_uri}/individual_id> \"#{individual_id}\" ;\n"
  print "\t<#{fixed_uri}/organ_id> \"#{organ_id}\" ;\n"
  print "\t<#{fixed_uri}/material_id> \"#{material_id}\" ;\n"
  print "\t<#{fixed_uri}/compound_name> \"#{cpd_name}\" ;\n"
  print "\t<#{fixed_uri}/compound_abbr> \"#{cpd_abbr}\" ;\n"
  print "\t<#{fixed_uri}/compound_no> \"#{cpd_no}\" ;\n"
  print "\t<#{fixed_uri}/cas_number> <#{fixed_uri}/#{casnum}> ;\n"
  print "\t<#{fixed_uri}/kegg_drug> <#{fixed_uri}/#{keggdr}> ;\n"
  print "\t<#{fixed_uri}/kegg_compound> <#{fixed_uri}/#{keggcp}> ;\n"
  print "\t<#{fixed_uri}/organism> \"#{meta_line[0]}\" ;\n"
  print "\t<#{fixed_uri}/test_type> \"#{meta_line[1]}\" ;\n"
  print "\t<#{fixed_uri}/sin_rep_type> \"#{meta_line[2]}\" ;\n"
  print "\t<#{fixed_uri}/sex_type> \"#{meta_line[3]}\" ;\n"
  print "\t<#{fixed_uri}/strain_type> \"#{meta_line[4]}\" ;\n"
  print "\t<#{fixed_uri}/adm_route_type> \"#{meta_line[5]}\" ;\n"
  print "\t<#{fixed_uri}/animal_age_week> \"#{meta_line[6]}\" ;\n"
  print "\t<#{fixed_uri}/sacri_period> \"#{meta_line[7]}\" ;\n"
  print "\t<#{fixed_uri}/dose> \"#{meta_line[8]}\" ;\n"
  print "\t<#{fixed_uri}/dose_unit> \"#{meta_line[9]}\" ;\n"
  print "\t<#{fixed_uri}/dose_level> \"#{meta_line[10]}\" ;\n"


end




















