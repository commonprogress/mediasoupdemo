#! /bin/bash

SCRIPT_DIR=${BASH_SOURCE%/*}
. "$SCRIPT_DIR/version.sh"

if [ -e depot_tools ]; then
	pushd depot_tools
	git pull
	popd
else
	git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
fi

export PATH=$PATH:$PWD/depot_tools

if [ -e webrtc_checkout ]; then
	pushd webrtc_checkout/
else
	mkdir webrtc_checkout
	pushd webrtc_checkout/
	fetch --nohooks webrtc_android
fi

pushd src > /dev/null

#if [ "$WEBRTC_COMMIT" == "" ]; then
#	git checkout remotes/branch-heads/$WEBRTC_RELEASE
        git checkout -b $WEBRTC_RELEASE refs/remotes/branch-heads/4606
#else
#	git checkout $WEBRTC_COMMIT
#fi
#yes | gclient sync -D
yes | gclient sync

#sed s/sudo/echo\ sudo/g build/install-build-deps-android.sh > build/install-build-deps-android-nosudo.sh
#. build/install-build-deps-android-nosudo.sh --quick-check
#. build/android/envsetup.sh

#for PATCH in ../../patch/*.patch; do
#  patch -p1 < $PATCH
#done

#In Linux Debian Stretch with GCC 6.3 this works:
#gn gen out/m94 --args='is_debug=false is_component_build=false is_clang=false rtc_include_tests=false rtc_use_h264=true use_rtti=true use_custom_libcxx=false treat_warnings_as_errors=false use_ozone=true'

export ARGS="is_debug=false is_component_build=false libyuv_include_tests=false rtc_include_tests=false rtc_build_tools=false is_chrome_branded=true rtc_use_h264=true proprietary_codecs=true rtc_enable_protobuf=false ffmpeg_branding=\"Chrome\" is_component_ffmpeg=true use_rtti=true use_custom_libcxx=false use_custom_libcxx_for_host=false treat_warnings_as_errors=false use_ozone=true"
#export ARGS="is_debug=false rtc_include_tests=false rtc_build_examples=false rtc_build_tools=false use_rtti=true rtc_use_h264=true proprietary_codecs=true use_custom_libcxx=false"

gn gen out/linux-x86_64 -args="target_os=\"linux\" target_cpu=\"x64\" $ARGS"
gn args out/linux-x86_64 --list=proprietary_codecs
gn args out/linux-x86_64 --list=rtc_use_h264
gn args out/linux-x86_64 --list=ffmpeg_branding
gn args out/linux-x86_64 --list=is_chrome_branded
ninja -C out/linux-x86_64

gn gen out/android-i386 -args="target_os=\"android\" target_cpu=\"x86\" $ARGS"
gn args out/android-i386 --list=proprietary_codecs
gn args out/android-i386 --list=rtc_use_h264
gn args out/android-i386 --list=ffmpeg_branding
gn args out/android-i386 --list=is_chrome_branded
#gn args out/android-i386 --list=build_with_chromium
ninja -C out/android-i386

gn gen out/android-x86_64 -args="target_os=\"android\" target_cpu=\"x64\" $ARGS"
gn args out/android-x86_64 --list=proprietary_codecs
gn args out/android-x86_64 --list=rtc_use_h264
gn args out/android-x86_64 --list=ffmpeg_branding
gn args out/android-x86_64 --list=is_chrome_branded
#gn args out/android-x86_64 --list=build_with_chromium
ninja -C out/android-x86_64

gn gen out/android-armv7 -args="target_os=\"android\" target_cpu=\"arm\" $ARGS"
gn args out/android-armv7 --list=proprietary_codecs
gn args out/android-armv7 --list=rtc_use_h264
gn args out/android-armv7 --list=ffmpeg_branding
gn args out/android-armv7 --list=is_chrome_branded
#gn args out/android-armv7 --list=build_with_chromium
ninja -C out/android-armv7

gn gen out/android-arm64 -args="target_os=\"android\" target_cpu=\"arm64\" $ARGS"
gn args out/android-arm64 --list=proprietary_codecs
gn args out/android-arm64 --list=rtc_use_h264
gn args out/android-arm64 --list=ffmpeg_branding
gn args out/android-arm64 --list=is_chrome_branded
#gn args out/android-arm64 --list=build_with_chromium
ninja -C out/android-arm64

popd > /dev/null
popd > /dev/null
