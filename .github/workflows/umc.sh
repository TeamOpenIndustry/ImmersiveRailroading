#!/bin/bash

branch=$1
version=$2
tag=$3

if [ "$tag" == "stable" ]; then
  umcVersion="$version"
elif [ "$tag" == "latest" ]; then
  git clone https://github.com/TeamOpenIndustry/UniversalModCore.git --depth=1 --branch $branch
  umcVersion="$version-$(cd UniversalModCore && git rev-parse --short=6 HEAD)"
  rm -rf UniversalModCore
else
  umcVersion="$version-$tag"
fi

sed -i build.gradle -e "s/umcVersion = .*/umcVersion = '$umcVersion'/"
