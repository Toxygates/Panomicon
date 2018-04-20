#!/bin/bash

export PATH=$PATH:/usr/local/bin


KEGGBASE=/home/nibioadmin/toxygates/kegg_rdf
OUTDIR=$KEGGBASE/download
RDFDIR=$KEGGBASE/rdf
SPLIT=$KEGGBASE/split_kegg_genes.scala
B2R=$KEGGBASE/b2r/bio2rdf-scripts-release3

mkdir -p $OUTDIR

#Copy fresh files from the latest regular download
#These files are available on the cluster (e.g. 10.100.0.34)
KEGGFTP=/data/bio/db/ftp/kegg/lftp
cp $KEGGFTP/genes/organisms/hsa/T01001.ent.gz $OUTDIR
cp $KEGGFTP/genes/organisms/mmu/T01002.ent.gz $OUTDIR
cp $KEGGFTP/genes/organisms/rno/T01003.ent.gz $OUTDIR

cp $KEGGFTP/xml/kgml/metabolic/ko.tar.gz $OUTDIR

cd $OUTDIR
tar xzf ko.tar.gz
gzip -d *ent.gz

#Split up gene entries into individual files (a format expected by the
#bio2rdf scripts)
scala $SPLIT T01001.ent hsa
scala $SPLIT T01002.ent mmu
scala $SPLIT T01003.ent rno

mkdir -p $RDFDIR
rm -r $RDFDIR/*

#Generate RDF using bio2rdf tools
cd $B2R
php runparser.php parser=kegg files=pathway,genes indir=$OUTDIR outdir=$RDFDIR output_format=nt

#Un-escape single quotes (due to Bio2RDF bug).
#Filter the generated files to reduce size. Keep only the parts we need
cd $RDFDIR
cat kegg-pathway.nt | sed "s/\\\'/'/g" | grep "kegg:map" > kegg-pathway.f.nt
cat kegg-genes.nt  | sed "s/\\\'/'/g" | egrep "vocabulary:pathway|ncbigene" > kegg-genes.f.nt

cat kegg-pathway.f.nt kegg-genes.f.nt > kegg-pathways-genes.f.nt

#This directory will have too many files for a single rm command
find $OUTDIR -type f -print0 | xargs -0 rm


