branch=$1

if [ -z "$branch" ]; then
		branch="forge_1.12.2"
fi

unlink $PWD/src/main/java/cam72cam/immersiverailroading/thirdparty

rm -rf ./ImmersiveRailroadingMod
rm -rf ./ImmersiveRailroadingIntegration

mkdir -p run

git clone --branch $branch git@github.com:TeamOpenIndustry/ImmersiveRailroadingIntegration.git
git clone --branch $branch git@github.com:TeamOpenIndustry/ModCoreMod.git ImmersiveRailroadingMod

ln -s $PWD/src/main/java/cam72cam/immersiverailroading $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading
ln -s $PWD/ImmersiveRailroadingIntegration/src/main/java/cam72cam/immersiverailroading/thirdparty $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading/thirdparty
ln -s $PWD/src/main/resources $PWD/ImmersiveRailroadingMod/src/main/resources
ln -s $PWD/run $PWD/ImmersiveRailroadingMod/run

pushd ImmersiveRailroadingMod
		bash setup.sh $branch immersiverailroading ImmersiveRailroading 1.6.1 cam72cam.immersiverailroading.ImmersiveRailroading
		echo "apply from: '../ImmersiveRailroadingIntegration/dependencies.gradle'" >> build.gradle
popd
