#!/bin/bash

function updateBatch {
	gzip -d $2/*gz
	./update_batch.sh $1 $2/*meta.tsv
	gzip $2/*.tsv $2/*.csv
}

updateBatch HumanVitroLiver Human/in_vitro 
updateBatch RatVitroLiver Rat/in_vitro
updateBatch RatVivoKidneyRepeat Rat/in_vivo/Kidney/Repeat
updateBatch RatVivoKidneySingle Rat/in_vivo/Kidney/Single
updateBatch RatVivoLiverRepeat Rat/in_vivo/Liver/Repeat
updateBatch RatVivoLiverSingle Rat/in_vivo/Liver/Single

