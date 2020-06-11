执行的gn 命令设置参数如下：
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
ninja -C out/android-i386

gn gen out/android-x86_64 -args="target_os=\"android\" target_cpu=\"x64\" $ARGS"
gn args out/android-x86_64 --list=proprietary_codecs
gn args out/android-x86_64 --list=rtc_use_h264
gn args out/android-x86_64 --list=ffmpeg_branding
gn args out/android-x86_64 --list=is_chrome_branded
ninja -C out/android-x86_64

gn gen out/android-armv7 -args="target_os=\"android\" target_cpu=\"arm\" $ARGS"
gn args out/android-armv7 --list=proprietary_codecs
gn args out/android-armv7 --list=rtc_use_h264
gn args out/android-armv7 --list=ffmpeg_branding
gn args out/android-armv7 --list=is_chrome_branded
ninja -C out/android-armv7

gn gen out/android-arm64 -args="target_os=\"android\" target_cpu=\"arm64\" $ARGS"
gn args out/android-arm64 --list=proprietary_codecs
gn args out/android-arm64 --list=rtc_use_h264
gn args out/android-arm64 --list=ffmpeg_branding
gn args out/android-arm64 --list=is_chrome_branded
ninja -C out/android-arm64

需要修改的内容：
1. 为 FFmpeg 开启 H.264 的编译选项，需要修改 third_party/ffmpeg 下的脚本
    a. third_party/ffmpeg/ffmpeg_generated.gni 将 h264 相关的源码添加到 ffmpeg_c_sources 中
        方法一：ffmpeg_generated.gni第15行 use_linux_config参数加上is_android修改成如下，use_linux_config = is_linux || is_fuchsia || is_android

    b. 修改 third_party/ffmpeg/chromium/config/Chromium/{ABI}/config.h 中 #define CONFIG_H264_DECODER 的宏定义
        由于上面使用的 ffmpeg_branding=\"Chrome\" 我们需要修改hird_party/ffmpeg/chromium/config/Chrome/{平台}/{ABI}/config.h的#define CONFIG_H264_DECODER  由于默认是#define CONFIG_H264_DECODER 0 我们需要改成#define CONFIG_H264_DECODER 1 在config.h文件的797行 需要把平台下各个{ABI}下config.h文件都修改

    c. 由于 FFmpeg 废弃了 av_register_all，所以需要静态的注册 H.264。third_party/ffmpeg/chromium/config/Chromium/android/{ABI}/libavcodec/parser_list.c 和 third_party/ffmpeg/chromium/config/Chromium/android/{ABI}/libavcodec/codec_list.c 分别添加 h264 的 parser 和 decoder
         由于上面使用的 ffmpeg_branding=\"Chrome\" 我们需要修改hird_party/ffmpeg/chromium/config/Chrome/{平台}/{ABI}/libavcodec/下的parser_list.c和codec_list.c文件 ，需要把平台下各个{ABI}都修改：
        修改如下：parser_list.c 添加 &ff_h264_parser,,   codec_list.c 添加 &ff_h264_decoder,
        



    执行上面gn 命令就支持h264 编解码了  

注意：需要生成的libffmepg.so 和 libjingle_peerconnection_so.so 一起打包使用，或者需要生产的libffmepg.so和libwebrtc.a 一起再次打包成so库

上面已经可以用h264 下面是对h264支持的扩展：

2，h264 支持软解：尽管我们已经将 H.264 的软解添加到 FFmpeg 中了，但是 WebRTC 还是没有调用到这些代码。这里我们可以参考源码中对于 VP9 的处理，依样画葫芦
      a. 参照 sdk/android/api/org/webrtc/VP9Decoder.java 和 sdk/android/api/org/webrtc/VP9Encoder.java 实现 H.264 的 Java 层 Wrapper  方法：在sdk/android/api/org/webrtc/LibvpxH264Decoder.java创建
copy同目录下的LibvpxVp9Decoder.java改名而来，把VP9名字改成H264

     在sdk/android/api/org/webrtc/LibvpxH264Encoder.java创建
copy同目录下的LibvpxVp9Encoder.java改名而来，把VP9名字改成H264


     b. 参照sdk/android/src/jni/vp9_codec.cc 实现 H.264 的 native 代码  在sdk/android/src/jni/h264_codec.cc
copy同目录下的vp9_codec.cc改名而来，把VP9名字改成H264



      c. sdk/android/BUILD.gn 中分别添加 generate_jni 和 rtc_static_library 任务，并添加对应的 java 和 cpp 文件
如：把包含LibvpxVp9Decoder.java、LibvpxVp9Encoder.java、vp9_codec.cc的所有段复制一份，并改成前面我们copy出来的文件名
 










       d. 在 sdk/android/api/org/webrtc/SoftwareVideoDecoderFacoty.java 和 

sdk/android/api/org/webrtc/SoftwareVideoEncoderFacoty.java 

中分别注册 H.264 并添加创建 codec 的代码
      
      注意：WebRTC Android JNI 接口的 C 层函数定义，都是通过 Python 脚本 base/android/jni_generator/jni_generator.py 生成的，生成的代码在 out/{destination}/gen/sdk/android/generated_xxx_jni/jni 目录下。我们需要通过第c步生成 H.264 的 JNI 函数，并在第d步中引用生成的头文件。
另注：我们在将 H.264 的软编(解)码添加到工厂类里时，需要传入一个 Map 作为参数。sdk/android/src/java/org/webrtc/H264Utils.java 提供了这些生成参数的方法。这里的参数会影响协商时生成的 SDP，要根据业务实现。

3，h264硬解支持更多机型：
      a. 编码支持更多类型在sdk/android/api/org/webrtc/HardwareVideoEncoderFactory.java 类中isHardwareSupportedInCurrentSdkH264()方法中添加支持更多硬件的设备信息 三种方法 ，一，所以类型都返ture：

二：继续增加类型：

三：加设备白名单：如H264_HW_EXCEPTION_MODELS     机型增加机型
        b.增加设备解码类型在类 android/src/java/org/webrtc/MediaCodecVideoDecoderFactory.java中isH264HighProfileSupported 方法中出来 处理方法如上


参照文献：https://blog.dio.wtf/post/enable-h264-codec-for-webrtc/
https://blog.csdn.net/u011382962/article/details/94734794?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.nonecase&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.nonecase
https://blog.csdn.net/rjc_lihui/article/details/102581072

让h264 硬解支持更多机型：
https://download.csdn.net/download/lerous/12272420?utm_medium=distribute.pc_relevant_t0.none-task-download-BlogCommendFromMachineLearnPai2-1.nonecase
https://blog.csdn.net/lerous/article/details/105124553
https://blog.csdn.net/liwenlong_only/article/details/88713673?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.nonecase&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.nonecase