param (
	[string]$branch = "forge_1.12.2"
)

if(Test-Path -Path UniversalModCore ){
		Remove-Item -Recurse -Force ./UniversalModCore
}
if(Test-Path -Path ImmersiveRailroadingIntegration ){
		Remove-Item -Recurse -Force ./ImmersiveRailroadingIntegration
}

git clone --branch $branch git@github.com:TeamOpenIndustry/UniversalModCore.git
git clone --branch $branch git@github.com:TeamOpenIndustry/ImmersiveRailroadingIntegration.git

./UniversalModCore/template/setup.ps1 $branch immersiverailroading ImmersiveRailroading 1.6.1 cam72cam.immersiverailroading.ImmersiveRailroading

./ImmersiveRailroadingIntegration/setu.ps1 $branch
