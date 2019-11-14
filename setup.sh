branch=$1
useHttps=$2

if [ -z "$branch" ]; then
	branch="forge_1.12.2"
fi

gitPfx="git@github.com:"
if [ ! -z "$useHttps" ]; then
	gitPfx="https://github.com/"
fi

rm -rf ./UniversalModCore
rm -rf ./ImmersiveRailroadingIntegration

git clone --branch $branch ${gitPfx}TeamOpenIndustry/UniversalModCore.git
git clone --branch $branch ${gitPfx}TeamOpenIndustry/ImmersiveRailroadingIntegration.git

./UniversalModCore/template/setup.sh $branch immersiverailroading ImmersiveRailroading 1.7.1 cam72cam.immersiverailroading.ImmersiveRailroading

./ImmersiveRailroadingIntegration/setup.sh $branch $useHttps
