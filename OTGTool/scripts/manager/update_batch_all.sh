#!/bin/bash

./update_batch.sh HumanVitroLiver Human/in_vitro/*meta.tsv.gz 
./update_batch.sh RatVitroLiver Rat/in_vitro/*meta.tsv.gz
./update_batch.sh RatVivoKidneyRepeat Rat/in_vivo/Kidney/Repeat/*meta.tsv.gz
./update_batch.sh RatVivoKidneySingle Rat/in_vivo/Kidney/Single/*meta.tsv.gz
./update_batch.sh RatVivoLiverRepeat Rat/in_vivo/Liver/Repeat/*meta.tsv.gz
./update_batch.sh RatVivoLiverSingle Rat/in_vivo/Liver/Single/*meta.tsv.gz

