#! /bin/bash

SCRIPT_DIR=${BASH_SOURCE%/*}
. "$SCRIPT_DIR/version.sh"

if [ -e depot_tools ]; then
	pushd depot_tools > /dev/null
	git pull
	popd > /dev/null
else
	git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
fi

export PATH=$PATH:$PWD/depot_tools

if [ -e webrtc_checkout ]; then
	pushd webrtc_checkout/ > /dev/null
else
	mkdir webrtc_checkout
	pushd webrtc_checkout/ > /dev/null
	fetch --nohooks webrtc_ios
fi

#gclient sync

pushd src > /dev/null
#if [ "$WEBRTC_COMMIT" == "" ]; then
#	git checkout remotes/branch-heads/$WEBRTC_RELEASE
        git checkout -b $WEBRTC_RELEASE refs/remotes/branch-heads/4606
#else
#	git checkout $WEBRTC_COMMIT
#fi
#gclient sync -D
gclient sync

#for PATCH in ../../patch/*.patch; do
#  patch -p1 < $PATCH
#done

#In OSX 10.14.16 this works:
#gn gen out/m94 --args='is_debug=false is_component_build=false is_clang=true rtc_include_tests=false rtc_use_h264=true use_rtti=true mac_deployment_target="10.11" use_custom_libcxx=false'


export ARGS="is_debug=false rtc_include_tests=false rtc_build_examples=false rtc_build_tools=false"
gn gen out/osx-x86_64 -args="target_os=\"mac\" target_cpu=\"x64\" $ARGS"
ninja -C out/osx-x86_64

gn gen out/ios-x86_64 -args="target_os=\"ios\" target_cpu=\"x64\" $ARGS"
ninja -C out/ios-x86_64

gn gen out/ios-armv7 -args="target_os=\"ios\" target_cpu=\"arm\" $ARGS"
ninja -C out/ios-armv7

gn gen out/ios-arm64 -args="target_os=\"ios\" target_cpu=\"arm64\" $ARGS"
ninja -C out/ios-arm64

popd > /dev/null
popd > /dev/null

