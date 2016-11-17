This directory contains example data in a format that can be uploaded to Toxygates.

For uploads to the version of Toxygates hosted at NIBIOHN, please note that while we will make reasonable efforts to protect your data and keep it secure, we are in no way responsible for any damages or incidents that may occur as a result of data corruption, loss or theft. You agree to use this functionality (as well as all other functionality in Toxygates) at your own risk. 

At least two files must be uploaded: a metadata .tsv file that describes each sample, and a file with normalised expression data (.csv).
In addition, an affymetrix calls file may optionally be uploaded (also .csv).

Accordingly, this directory contains the following files:

vitamin_a_metadata.tsv
vitamin_a_expr.csv
vitamin_a_call.csv

The format of each file is described below.

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
