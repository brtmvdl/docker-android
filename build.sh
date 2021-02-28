#!/bin/bash

if [[ -z "$@" ]]; then
TAGS=$(ls -d */ | sed "s/\///g")
else
TAGS="$@ latest"
fi

for tag in $TAGS; do

echo
echo
echo --- TAG $tag ---
[[ $tag = "latest" ]] && tag=$(cat latest) && echo latest is $tag
echo

cd $tag
docker build -t tmvdl/android:$tag .
cd ..

echo
echo --- TAG $tag OK ---
echo
echo

done
