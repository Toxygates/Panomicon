#WGCNA integration for panomicon
#Adapted from the WGCNA tutorial (mainly parts 1, 2) at: 
#https://horvath.genetics.ucla.edu/html/CoexpressionNetwork/Rpackages/WGCNA/Tutorials/index.html

#inputs that must be set by caller:
#imageDir - directory where images are output
#datExpr0 - gene expression data matrix (sample-major)
#traitData - bio parameters (parameter-major)
#cutHeight - cutoff height for clustering dendrogram

#parameters
minSize = 10


#=====================================================================================
#
#  From step 1
#
#=====================================================================================


# Load the WGCNA package
library(WGCNA);
# The following setting is important, do not omit.
options(stringsAsFactors = FALSE);



gsg = goodSamplesGenes(datExpr0, verbose = 3);
gsg$allOK



if (!gsg$allOK)
{
  # Optionally, print the gene and sample names that were removed:
  if (sum(!gsg$goodGenes)>0) 
     printFlush(paste("Removing genes:", paste(names(datExpr0)[!gsg$goodGenes], collapse = ", ")));
  if (sum(!gsg$goodSamples)>0) 
     printFlush(paste("Removing samples:", paste(rownames(datExpr0)[!gsg$goodSamples], collapse = ", ")));
  # Remove the offending genes and samples from the data:
  datExpr0 = datExpr0[gsg$goodSamples, gsg$goodGenes]
}


sampleTree = hclust(dist(datExpr0), method = "average");
png(file = paste(imageDir, "sampleClustering.png", sep="/"), 1600, 1200);
par(cex = 0.6);
par(mar = c(0,4,2,0))
plot(sampleTree, main = "Sample clustering to detect outliers", sub="", xlab="", cex.lab = 1.5, 
     cex.axis = 1.5, cex.main = 2)

     
#Select cutoff
# Plot a line to show the cut
abline(h = cutHeight, col = "red");
#Close the graphics device (PNG file)     
dev.off()

# Determine cluster under the line
clust = cutreeStatic(sampleTree, cutHeight = cutHeight, minSize = minSize)
table(clust)
# clust 1 contains the samples we want to keep.
keepSamples = (clust==1)
datExpr = datExpr0[keepSamples, ]
nGenes = ncol(datExpr)
nSamples = nrow(datExpr)

allTraits = traitData
# Form a data frame analogous to expression data that will hold the clinical traits.
samples = rownames(datExpr);
traitRows = match(samples, allTraits[["sample_id"]]);
datTraits = allTraits[traitRows, -1];
rownames(datTraits) = allTraits[traitRows, 1];

collectGarbage();


# Re-cluster samples
sampleTree2 = hclust(dist(datExpr), method = "average")
# Convert traits to a color representation: white means low, red means high, grey means missing entry
traitColors = numbers2colors(datTraits, signed = FALSE);

png(file = paste(imageDir, "dendrogramTraits.png", sep="/"), 1600, 1200);
# Plot the sample dendrogram and the colors underneath.
plotDendroAndColors(sampleTree2, traitColors,
                    groupLabels = names(datTraits), 
                    main = "Sample dendrogram and trait heatmap")
dev.off()


#=====================================================================================
#
#  From step 2
#
#=====================================================================================

# Choose a set of soft-thresholding powers
powers = c(c(1:10), seq(from = 12, to=20, by=2))
# Call the network topology analysis function
sft = pickSoftThreshold(datExpr, powerVector = powers, verbose = 5)

png(file = paste(imageDir, "softThreshold.png", sep="/"), 1600, 1200);
par(mfrow = c(1,2));
cex1 = 0.9;


# Scale-free topology fit index as a function of the soft-thresholding power
plot(sft$fitIndices[,1], -sign(sft$fitIndices[,3])*sft$fitIndices[,2],
     xlab="Soft Threshold (power)",ylab="Scale Free Topology Model Fit,signed R^2",type="n",
     main = paste("Scale independence"));
text(sft$fitIndices[,1], -sign(sft$fitIndices[,3])*sft$fitIndices[,2],
     labels=powers,cex=cex1,col="red");
# this line corresponds to using an R^2 cut-off of h
abline(h=0.90,col="red")
# Mean connectivity as a function of the soft-thresholding power
plot(sft$fitIndices[,1], sft$fitIndices[,5],
     xlab="Soft Threshold (power)",ylab="Mean Connectivity", type="n",
     main = paste("Mean connectivity"))
text(sft$fitIndices[,1], sft$fitIndices[,5], labels=powers, cex=cex1,col="red")

dev.off()

net = blockwiseModules(datExpr, power = 8,
                       TOMType = "unsigned", minModuleSize = 30,
                       reassignThreshold = 0, mergeCutHeight = 0.25,
                       numericLabels = TRUE, pamRespectsDendro = FALSE,
                       saveTOMs = TRUE,
                       saveTOMFileBase = "panomiconTOM", 
                       verbose = 3)
                       
moduleLabels = net$colors
moduleColors = labels2colors(net$colors)
MEs = net$MEs;
geneTree = net$dendrograms[[1]];


png(file = paste(imageDir, "modules.png", sep="/"), 1600, 1200);
# Convert labels to colors for plotting
mergedColors = labels2colors(net$colors)
# Plot the dendrogram and the module colors underneath
plotDendroAndColors(net$dendrograms[[1]], mergedColors[net$blockGenes[[1]]],
                    "Module colors",
                    dendroLabels = FALSE, hang = 0.03,
                    addGuide = TRUE, guideHang = 0.05)
dev.off()

#save(MEs, moduleLabels, moduleColors, geneTree, 
#  file = "Bushel-02-networkConstruction-auto.RData")


#=====================================================================================
#
#  From step 3
#
#=====================================================================================


# Define numbers of genes and samples
nGenes = ncol(datExpr);
nSamples = nrow(datExpr);
# Recalculate MEs with color labels
MEs0 = moduleEigengenes(datExpr, moduleColors)$eigengenes
MEs = orderMEs(MEs0)
moduleTraitCor = cor(MEs, datTraits, use = "p");
moduleTraitPvalue = corPvalueStudent(moduleTraitCor, nSamples);

probes = names(datExpr)

geneInfo0 = data.frame(substanceBXH = probes,
                      moduleColor = moduleColors)
                      
                      # Order modules by their significance for weight
#modOrder = order(-abs(cor(MEs, weight, use = "p")));
# Add module membership information in the chosen order
#for (mod in 1:ncol(geneModuleMembership))
#{
#  oldNames = names(geneInfo0)
#  geneInfo0 = data.frame(geneInfo0, geneModuleMembership[, modOrder[mod]], 
#                         MMPvalue[, modOrder[mod]]);
#  names(geneInfo0) = c(oldNames, paste("MM.", modNames[modOrder[mod]], sep=""),
#                       paste("p.MM.", modNames[modOrder[mod]], sep=""))
#}

# Order the genes in the geneInfo variable by module color
geneOrder = order(geneInfo0$moduleColor);
geneInfo = geneInfo0[geneOrder, ]

write.csv(geneInfo, file = "geneInfo.csv")
