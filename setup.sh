branch=$1

if [ -z "$branch" ]; then
		branch="forge_1.12.2"
fi

rm -rf ./TrackAPI
rm -rf ./UniversalModCore
rm -rf ./ImmersiveRailroadingIntegration

git clone --branch $branch git@github.com:TeamOpenIndustry/TrackAPI.git
git clone --branch $branch git@github.com:TeamOpenIndustry/UniversalModCore.git
git clone --branch $branch git@github.com:TeamOpenIndustry/ImmersiveRailroadingIntegration.git

./UniversalModCore/template/setup.sh $branch immersiverailroading ImmersiveRailroading 1.6.1 cam72cam.immersiverailroading.ImmersiveRailroading
sed -i build.gradle -e "s,^dependencies {,apply from: 'ImmersiveRailroadingIntegration/dependencies.gradle'\n\0,"
