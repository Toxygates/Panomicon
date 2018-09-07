#!/bin/bash

OUT=../tglobal.jar
cd classes
rm -i $OUT
jar cf $OUT t/global/*.class
