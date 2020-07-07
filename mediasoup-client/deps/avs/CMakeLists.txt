cmake_minimum_required(VERSION 3.5)

#project(avs LANGUAGES CXX)
project(avs)

# Set version number.
set(avs_VERSION_MAJOR 3)
set(avs_VERSION_MINOR 0)
set(avs_VERSION_PATCH 8)

# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置C语言标准
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

#判断编译器类型,如果是gcc编译器,则在编译选项中加入c++11支持
#if(CMAKE_COMPILER_IS_GNUCXX)
#    set(CMAKE_CXX_FLAGS "-std=c++11 ${CMAKE_CXX_FLAGS}")
#    message(STATUS "optMalformedional:-std=c++11")
#endif(CMAKE_COMPILER_IS_GNUCXX)

# Project configuration.
set(LIBWEBRTC_INCLUDE_PATH "" CACHE STRING "libwebrtc include path")
set(LIBWEBRTC_BINARY_PATH "" CACHE STRING "libwebrtc binary path")

if (NOT LIBWEBRTC_INCLUDE_PATH)
    message(FATAL_ERROR "LIBWEBRTC_INCLUDE_PATH not provided")
endif ()

if (NOT LIBWEBRTC_BINARY_PATH)
    message(FATAL_ERROR "LIBWEBRTC_BINARY_PATH not provided")
endif ()

message("\n=========== libavs Build Configuration ===========\n")
#message(STATUS "MEDIASOUPCLIENT_BUILD_TESTS : " ${MEDIASOUPCLIENT_BUILD_TESTS})
#message(STATUS "MEDIASOUPCLIENT_LOG_TRACE   : " ${MEDIASOUPCLIENT_LOG_TRACE})
#message(STATUS "MEDIASOUPCLIENT_LOG_DEV     : " ${MEDIASOUPCLIENT_LOG_DEV})
message(STATUS "LIBWEBRTC_INCLUDE_PATH      : " ${LIBWEBRTC_INCLUDE_PATH})
message(STATUS "LIBWEBRTC_BINARY_PATH       : " ${LIBWEBRTC_BINARY_PATH})
message(STATUS "PROJECT_NAME                : " ${PROJECT_NAME})
message(STATUS "PROJECT_SOURCE_DIR          : " ${PROJECT_SOURCE_DIR})
message("")

# GCC >= 4.9 is required due to std::regex use.
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS 4.9)
        message(FATAL_ERROR "GCC version must be at least 4.9!")
    endif ()
endif ()

set(
        SOURCE_FILES_C
        src/audio_effect/aueffect.c
        src/base/base.c
        src/dict/dict.c
        src/extcodec/extcodec.c
        src/flowmgr/device.c
        src/flowmgr/flowmgr.c
        src/flowmgr/marshal.c
        src/log/log.c
        src/mediamgr/mediamgr.c
        src/mediamgr/sound.c
        src/mediamgr/mm_platform_android.c
        src/msystem/device.c
        src/msystem/msystem.c
        src/msystem/msys_default.c
        src/queue/locked_queue.c
        src/queue/packet_queue.c
        src/sem/semaphore.c
        src/string/wordexp.c
        src/version/version.c
        )

set(
        SOURCE_FILES
        src/audio_effect/chorus.cpp
        src/audio_effect/reverb.cpp
        src/audio_effect/pitch_shift.cpp
        src/audio_effect/pace_shift.cpp
        src/audio_effect/vocoder.cpp
        src/audio_effect/auto_tune.cpp
        src/audio_effect/harmonizer.cpp
        src/audio_effect/normalizer.cpp
        src/audio_effect/pitch_cycler.cpp
        src/audio_effect/pass_through.cpp
        src/audio_effect/find_pitch_lags.cpp
        src/audio_effect/time_scale.cpp
        src/audio_effect/biquad.cpp
        src/audio_effect/wav_interface.cpp
        src/audio_effect/pcm_interface.cpp
        src/audio_io/audio_io.cpp
        src/audio_io/mock/fake_audiodevice.cpp
        )

file(GLOB_RECURSE SOURCE_FILES_RE ${PROJECT_SOURCE_DIR}/contrib/re/src/*.c)

# include header files for the IDEs
file(GLOB_RECURSE HEADER_FILES_RE ${PROJECT_SOURCE_DIR}/contrib/re/src/*.h ${PROJECT_SOURCE_DIR}/contrib/re/include/*.h)

# Create target.
add_library(${PROJECT_NAME} STATIC ${SOURCE_FILES_C} ${SOURCE_FILES} ${SOURCE_FILES_RE} ${HEADER_FILES_RE})

message("\n=========== 33333333 ===========\n")

#if (${MEDIASOUPCLIENT_LOG_TRACE})
#    target_compile_definitions(${PROJECT_NAME}
#            PRIVATE MSC_LOG_TRACE=1
#            )
#endif ()
#
#if (${MEDIASOUPCLIENT_LOG_DEV})
#    target_compile_definitions(${PROJECT_NAME}
#            PRIVATE MSC_LOG_DEV=1
#            )
#endif ()

# Source Dependencies.
#add_subdirectory(re "${PROJECT_SOURCE_DIR}/contrib/re")

# Add some compile flags to our source files.
if (MSVC)
    message("\n=========== 66666666 ===========\n")
    set_source_files_properties(${SOURCE_FILES_C} ${SOURCE_FILES}
            PROPERTIES COMPILE_FLAGS "/W3 /WX")
else ()
    message("\n=========== 77777777 ===========\n")
    set_source_files_properties(${SOURCE_FILES_C} ${SOURCE_FILES}
            PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)
endif ()

# Private (implementation) header files.
target_include_directories(${PROJECT_NAME} PRIVATE
        "${PROJECT_SOURCE_DIR}/include"
        "${PROJECT_SOURCE_DIR}/src/audio_effect"
        "${PROJECT_SOURCE_DIR}/src/audio_io"
        "${PROJECT_SOURCE_DIR}/src/audio_io/mock"
        "${PROJECT_SOURCE_DIR}/src/flowmgr"
        "${PROJECT_SOURCE_DIR}/src/mediamgr"
        "${PROJECT_SOURCE_DIR}/src/msystem"
        "${PROJECT_SOURCE_DIR}/contrib/re"
        "${PROJECT_SOURCE_DIR}/contrib/re/include"
        )

# Public (interface) headers from dependencies.
target_include_directories(${PROJECT_NAME} PUBLIC
        "${LIBWEBRTC_INCLUDE_PATH}"
        "${LIBWEBRTC_INCLUDE_PATH}/rtc_base"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/abseil-cpp"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/ffmpeg"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/src"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/celt"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/include"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/silk"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/boringssl/src/include"
        )

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libffmpeg.so)

#add_library(re STATIC IMPORTED)
#set_target_properties(re
#        PROPERTIES IMPORTED_LOCATION
#        ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libre${CMAKE_STATIC_LIBRARY_SUFFIX})

message("\n=========== 4444444 ===========\n")

# Public (interface) dependencies.
if ("${ANDROID_ABI}" STREQUAL "")
    message("\n=========== 222222 ===========\n")
    target_link_libraries(${PROJECT_NAME} PUBLIC
#            re
            ffmpeg
            ${LIBWEBRTC_BINARY_PATH}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX}
            )
else ()
    message("\n=========== 11111111 ===========\n")
    # Add '-whole-archive' to keep symbols from peerconnection_jni.
    # https://stackoverflow.com/a/5687860/2085408
#    SET(re -Wl,--whole-archive ${PROJECT_SOURCE_DIR}/contrib/re/lib/${ANDROID_ABI}/libre${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
    SET(webrtc -Wl,--whole-archive ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
#    SET(opus -Wl,--whole-archive ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libopus${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
    target_link_libraries(${PROJECT_NAME} PUBLIC
#            re
            ffmpeg
            ${webrtc}
            )
endif ()

# Compile definitions for libwebrtc.
target_compile_definitions(${PROJECT_NAME} PUBLIC
        $<$<NOT:$<PLATFORM_ID:Windows>>:WEBRTC_POSIX>
        $<$<PLATFORM_ID:Windows>:WEBRTC_WIN>
        $<$<PLATFORM_ID:Darwin>:WEBRTC_MAC>
        )
message("\n=========== 55555555 ===========\n")
install(TARGETS avs DESTINATION lib)
#install(FILES ${HEADER_FILES} DESTINATION include/avs)