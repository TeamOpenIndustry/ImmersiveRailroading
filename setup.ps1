param (
	[string]$branch = "forge_1.12.2"
)

if(Test-Path -Path $PWD/src/main/java/cam72cam/immersiverailroading/thirdparty ){
		Remove-Item $PWD/src/main/java/cam72cam/immersiverailroading/thirdparty
}

Remove-Item -Recurse -Force ./ImmersiveRailroadingMod
Remove-Item -Recurse -Force ./ImmersiveRailroadingIntegration

if(!(Test-Path -Path run )){
		New-Item -ItemType directory run
}

git clone --branch $branch git@github.com:TeamOpenIndustry/ImmersiveRailroadingIntegration.git
git clone --branch $branch git@github.com:TeamOpenIndustry/UniversalModCoreMod.git ImmersiveRailroadingMod

New-Item -ItemType SymbolicLink -Target $PWD/src/main/java/cam72cam/immersiverailroading -Path $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading
New-Item -ItemType SymbolicLink -Target $PWD/ImmersiveRailroadingIntegration/src/main/java/cam72cam/immersiverailroading/thirdparty -Path $PWD/ImmersiveRailroadingMod/src/main/java/cam72cam/immersiverailroading/thirdparty
New-Item -ItemType SymbolicLink -Target $PWD/src/main/resources -Path $PWD/ImmersiveRailroadingMod/src/main/resources
New-Item -ItemType SymbolicLink -Target $PWD/run -Path $PWD/ImmersiveRailroadingMod/run

cd ImmersiveRailroadingMod
./setup.ps1 -branch $branch immersiverailroading ImmersiveRailroading 1.6.1 cam72cam.immersiverailroading.ImmersiveRailroading
Add-Content -Path build.gradle -Value "apply from: '../ImmersiveRailroadingIntegration/dependencies.gradle'"
