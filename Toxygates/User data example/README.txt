This directory contains example data in a format that can be uploaded to Toxygates.

For uploads to the version of Toxygates hosted at NIBIOHN, please note that while we will make reasonable efforts to protect your data and keep it secure, we are in no way responsible for any damages or incidents that may occur as a result of data corruption, loss or theft. You agree to use this functionality (as well as all other functionality in Toxygates) at your own risk. 

At least two files must be uploaded: a metadata .tsv file that describes each sample, and a file with normalised expression data (.csv).
In addition, an affymetrix calls file may optionally be uploaded (also .csv).

This directory contains the following example files:

vitamin_a_metadata.tsv
vitamin_a_expr.csv
vitamin_a_call.csv

The format of each file is described below.

In addition, there is an alternative metadata file, vitamin_a_metadata_full.tsv,
which shows how to upload biological sample data (see below). You do not need this advanced format for basic data uploads.

METADATA FILE
-------------
In this file, the first row is column headers. The order of the columns is irrelevant as long as all mandatory columns are present.
After the first row, each row describes a sample.

All columns are case sensitive.

The following columns are mandatory:

sample_id - Any string. This string must be unique across all of Toxygates. If your sample IDs are not unique, consider giving them a special prefix or suffix.

compound_name - Any string. Need not be unique. New compounds may be introduced.

dose_level - Control, Low, Middle or High.

exposure_time - hours or days, with a single space, for example: "2 hr", "9 hr" or "15 day" (without quotation marks).

platform_id - A string that names the platform. Currently, for user data, only Affymetrix data is supported, and only the following three platforms:

* Rat230_2
* HG-U133_Plus_2
* Mouse430_2

If you would like to use a different platform, please contact us with your request.

control_group - A unique number that associates treated samples with control samples. Samples in the same control group with the same exposure time will be considered to be a control-treated pair. In this way, control samples can potentially be shared across multiple compounds.

organism - For example, Rat, Mouse, Human, etc.

test_type - "in vitro" or "in vivo" (without quotation marks).

sin_rep_type - Single or Repeat. 

organ_id - The organ being studied, for example Liver, Kidney, etc. New organs may be introduced when necessary.

EXPRESSION DATA FILE
--------------------
In this CSV file, every row after the first one describes a probe (which must be a valid member of the platform) and every column after the first one describes a sample. Data is numerical.
Please do not forget to include the empty string "" at the start of the first row, as in the example. Every row must contain the same number of values.
The column headers correspond to sample IDs, as specified in the metadata file above.

AFFYMETRIX CALLS FILE (OPTIONAL)
--------------------------------
Similar to the expression data file, except that each data entry is either the letter "A", "P", or "M" for "absent", "present" or "marginal", respectively.

BIOLOGICAL AND EXTENDED PARAMETERS (OPTIONAL)
---------------------------------------------
To upload biological parameters (and other extended information about samples), a more extensive metadata file is used, where the additional columns (all optional) each specify a parameter. The mandatory columns are the same as in the basic example above. For missing values, the string NA may be used. 

An example can be seen in the file vitamin_a_metadata_full.tsv. This file may be used instead of the file vitamin_a_metadata.tsv from the above example.

The following additional parameters are available (all case sensitive):

ORGAN WEIGHT PARAMETERS

terminal_bw	Terminal body weight (g)
liver_wt	Liver weight (g)
kidney_total_wt	Kidney weight total (g)
kidney_wt_left	Kidney weight left (g)
kidney_wt_right	Kidney weight right (g)

HEMATOLOGY PARAMETERS

RBC	RBC (x 10^4/uL)
Hb	Hb (g/DL)
Ht	Ht (%)
MCV	MCV (fL)
MCH	MCH (pg)
MCHC	MCHC (%)
Ret	Ret (%)
Plat	Plat (x 10^4/uL)
WBC	WBC (x 10^2/uL)
Neu	Neutrophil (%)
Eos	Eosinophil (%)
Bas	Basophil (%)
Mono	Monocyte (%)
Lym	Lymphocyte (%)
PT	PT (s)
APTT	APTT (s)
Fbg	Fbg (mg/dL)

BIOCHEMISTRY PARAMETERS

ALP	ALP (IU/L)
TC	TC (mg/dL)
TBIL	TBIL (mg/dL)
DBIL	DBIL (mg/dL)
GLC	GLC (mg/dL)
BUN	BUN (mg/dL)
CRE	CRE (mg/dL)
Na	Na (meq/L)
K	K (meq/L)
Cl	Cl (meq/L)
Ca	Ca (mg/dL)
IP	IP (mg/dL)
TP	TP (g/dL)
RALB	RALB (g/dL)
AGratio	A / G ratio
AST	AST (GOT) (IU/L)
ALT	ALT (GPT) (IU/L)
LDH	LDH (IU/L)
GTP	gamma-GTP (IU/L)

CELL VIABILITY PARAMETERS

DNA	DNA (%)

OTHER PARAMETERS

exp_id	Experiment ID
group_id	Group ID
individual_id	Individual ID
material_id	Material ID
compound_abbr	Compound abbreviation
compound_no	Compound no
cas_number	CAS number
kegg_drug	KEGG drug
kegg_compound	KEGG compound
sex_type	Sex
strain_type	Strain
adm_route_type	Administration route
animal_age_week	Animal age (weeks)
dose_unit	Dose unit
medium_type	Medium type
product_information	Product information
cro_type	CRO type


