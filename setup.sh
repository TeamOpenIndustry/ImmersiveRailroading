unlink $PWD/src/main/java/cam72cam/immersiverailroading/thirdparty

rm -rf ./ImmersiveRailroadingMod
rm -rf ./ImmersiveRailroadingIntegration

git clone git@github.com:cam72cam/ImmersiveRailroadingIntegration.git
git clone git@github.com:cam72cam/ModCoreMod.git ImmersiveRailroadingMod

ln -s $PWD/src/main/java/cam72cam/immersiverailroading $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading
ln -s $PWD/ImmersiveRailroadingIntegration/src/main/java/cam72cam/immersiverailroading/thirdparty $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading/thirdparty
ln -s $PWD/src/main/resources $PWD/ImmersiveRailroadingMod/src/main/resources

pushd ImmersiveRailroadingMod
		bash setup.sh immersiverailroading ImmersiveRailroading 1.6.1 cam72cam.immersiverailroading.ImmersiveRailroading
		echo "apply from: '../ImmersiveRailroadingIntegration/dependencies.gradle'" >> build.gradle
popd
