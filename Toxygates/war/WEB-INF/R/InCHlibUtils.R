library(amap)
library(rjson)

#' Get the parent node of i
#'
#' @param i  node index
#' @param cluster a hclust object
#'
#' @examples
#' #hc <- hclust(dist(USArrests), "ave")
#' #getParent(-15, hc)   # 1
getParent <- function(i, cluster) {
  return(unname(which(cluster[['merge']] == i, arr.ind=TRUE)[, 1]))
}

#' Get the number of leafs in node i
#'
#' @param i       node index
#' @param cluster a hclust object
#'
#' @examples
#' #hc <- hclust(dist(USArrests), "ave")
#' #getCount(-15, hc)    # 1
#' #getCount(40, hc)     # 9
getCount <- function(i, cluster) {
  count <- 0
  if ( i < 0 ) {
    count <- 1
  } else {
    for( j in 1:2 ) {
      if (cluster[['merge']][i, j] < 0) {
        count = count + 1
      } else {
        count = count + getCount(cluster[['merge']][i, j], cluster)
      }
    }
  }
  return(count)
}

#' Get a dendrogram using the InCHlib format
#'
#' @param cluster   a hclust object
#' @param leafNames a character list containing the names of each leaf
#' @param values    a data.frame containing the data values (if row dendrogram)
#'
#' @examples
#' #hc <- hclust(dist(USArrests), "ave")
#' #hc.col <- hclust(dist(t(USArrests)), "ave")
#' #getDendro(hc, rownames(USArrests), USArrests)
#' #getDendro(hc.col, colnames(USArrests))
getDendro <- function(cluster, leafNames, values=NA) {
  getName <- function(id, leafNames) {
    if (length(id) == 1 && id < 0) {
      return(leafNames[abs(id)])
    } else {
      return(paste("nodes@", id, sep=""))
    }
  }
  nodes <- list()
  isRow <- FALSE
  if(is.data.frame(values)) {
    isRow <- TRUE
  }
  for(i in 1:nrow(cluster[['merge']])) {
    nodeName <- getName(i, leafNames)
    nodes[[nodeName]] <- list("left_child"=getName(cluster[['merge']][i, 1], leafNames),
                              "right_child"=getName(cluster[['merge']][i, 2], leafNames),
                              "count"=getCount(i, cluster),
                              "distance"=cluster[['height']][i])
    if (length(parent <- getParent(i, cluster))) 
      nodes[[nodeName]][["parent"]] <- getName(parent)
    for(j in 1:2) {
     id <- cluster[['merge']][i, j]
     if ( id < 0 ) {
      leaf <- getName(id, leafNames)
      nodes[[leaf]] <- list("count"=1,
                            "parent"=nodeName,
                            "distance"=0,
                            "objects"=list(leaf))
      if( isRow )
        nodes[[leaf]][['features']] <- unname(as.numeric(values[leaf, ]))
      }
    }
  }
  return(nodes)
}

#' Get a metadata list using the InCHlib format
#'
#' @param meta    a data.frame containing the metadata
#'
getMetadata <- function(meta) {
    return(lapply(apply(meta, 1, function(x) list(unname(x))), '[[', 1))
}

#' Create an InCHlib heatmap
#'
#' @param hclustRow   a hclust object
#' @param hclustCol   a hclust object
#' @param valDf       a data.frame containing the data values
#' @param metaDf      a data.frame containing the metadata
#'
#' @examples
#' hc <- hclust(dist(USArrests), "ave")
#' hc.col <- hclust(dist(t(USArrests)), "ave")
#' inch <- InCHlib(hc, hc.col, USArrests)
#' library(rjson)
#' writeLines(toJSON(inch), "heatmap.json")
InCHlib <- function(hclustRow, hclustCol, valDf, metaDf=NA) {
	# reorder cols
	valDf <- valDf[, hclustCol[['order']]]
  inch <- list('data'=list('nodes'=getDendro(hclustRow, rownames(valDf), valDf), 'feature_names'=colnames(valDf)), 'column_dendrogram'=list('nodes'=getDendro(hclustCol, colnames(valDf))))
	# reorder columns dendrogram
  inch[['column_dendrogram']][['nodes']] <- inch[['column_dendrogram']][['nodes']][c(colnames(valDf), names(subset(inch[['column_dendrogram']][['nodes']], grepl("nodes@", names(inch[['column_dendrogram']][['nodes']])))))]
  if (is.data.frame(metaDf)) {
    inch[['metadata']]=list("nodes"=getMetadata(metaDf), 'feature_names'=colnames(metaDf))
  }
	class(inch) <- "InCHlib"
  return(inch)
}

#' Create matrix with given vector
#'
#' @param data        a vector object containing the data values
#' @param rownames    a vector object containing the row names
#' @param colNames    a vector object containing the column names
#' @param byrow       boolean value whether to fill matrix by row (default=F)
#'
toMatrix <- function(data, rowNames, colNames, byrow=F) {
  mat <- matrix(data, length(rowNames), length(colNames), byrow=byrow)
  rownames(mat) <- rowNames
  colnames(mat) <- colNames
  return(mat)
}

#' Execute clustering
#'
#' @param data        a vector object containing the data values
#' @param rownames    a vector object containing the row names
#' @param colNames    a vector object containing the column names
#' @param rowMethod   a string object that gives which method to be used for row clustering.
#'                    This should be one of "ward.D", "ward.D2", "single", "complete", 
#'                                          "average", "mcquitty", "median" or "centroid".
#' @param rowDistance a string object that gives which distance to be used for row clustering.
#'                    This should be one of "euclidean", "maximum", "manhattan", "canberra", 
#'                                          "binary", "pearson", "abspearson", "correlation", 
#'                                          "abscorrelation", "spearman" or "kendall".
#' @param colMethod   a string object that gives which method to be used for column clustering.
#' @param colDistance a string object that gives which distance to be used for column clustering.
#'
getClusterAsJSON <- function(data, rowNames, colNames, rowMethod, rowDistance, colMethod, colDistance) {
  mat <- toMatrix(data, rowNames, colNames, TRUE)
  d <- Dist(mat, method=rowDistance)
  d.col <- Dist(t(mat), method=colDistance)
  hc <- hclust(d, method=rowMethod)
  hc.col <- hclust(d.col, method=colMethod)
  inch <- InCHlib(hc, hc.col, data.frame(mat))
  return(toJSON(inch))
}
