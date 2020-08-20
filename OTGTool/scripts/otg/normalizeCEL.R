#R code for normalizing affymetrix microarray data.
#Requires the affy package from bioconductor.

#Bioconductor installation
#source("https://bioconductor.org/biocLite.R")
#biocLite()

#Affy installation
#biocLite("affy")

library(affy)

args <- commandArgs(trailingOnly = TRUE)
dir <- args[1]

celpath <- paste(dir, "/celfiles", sep="")
  
files <- dir(path=celpath, pattern="\\.CEL$")
print(files)
affy.batch <- ReadAffy(filenames=paste(celpath, files, sep="/"))
mas5_calls <- mas5calls(affy.batch)
mas5_data <- data.frame(exprs(mas5(affy.batch, normalize=F)))
mas5_data2 <- mas5_data[-grep("AFFX",rownames(mas5_data)),,drop=F]
norm_mas5 <- apply(mas5_data2, 2, function(x){x/median(x)})
write.csv(norm_mas5, file=paste(dir, ".data.csv", sep=""))
write.exprs(mas5_calls, file= paste(dir, ".call", sep=""))
