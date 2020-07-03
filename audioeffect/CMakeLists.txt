cmake_minimum_required(VERSION 3.5)

#project(avs_so LANGUAGES CXX)
project(avs_so)

# Set version number.
set(avs_so_VERSION_MAJOR 3)
set(avs_so_VERSION_MINOR 0)
set(avs_so_VERSION_PATCH 8)


# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置C语言标准
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

#判断编译器类型,如果是gcc编译器,则在编译选项中加入c++11支持
#if(CMAKE_COMPILER_IS_GNUCXX)
#    set(CMAKE_CXX_FLAGS "-std=c++11 ${CMAKE_CXX_FLAGS}")
#    message(STATUS "optional:-std=c++11")
#endif(CMAKE_COMPILER_IS_GNUCXX)

set(
        SOURCE_DIR
        ${avs_so_SOURCE_DIR}/src/main/jni/
)

set(
        SOURCE_FILES
        ${SOURCE_DIR}/audio_effect.cc
        ${SOURCE_DIR}/media_manager.cc
)


# Create target.
add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

# Source Dependencies. override LIBWEBRTC_BINARY_PATH
# TODO(HaiyangWu) add libwebrtc.a without change CMakeLists in libmediasoupclient
# set(LIBWEBRTC_BINARY_PATH ${LIBWEBRTC_BINARY_ANDROID_PATH}/${ANDROID_ABI} CACHE STRING "libwebrtc binary path" FORCE)
add_subdirectory("deps/avs")

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libffmpeg.so)

# Add some compile flags to our source files.
set_source_files_properties(${SOURCE_FILES}
        PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)

include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/rtc_base")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/ffmpeg")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/abseil-cpp")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/src")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/celt")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/include")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/silk")
include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/boringssl/src/include")


target_include_directories(${PROJECT_NAME} PUBLIC
        "${SOURCE_DIR}/"
        "${PROJECT_SOURCE_DIR}/deps/avs/include"
        "${PROJECT_SOURCE_DIR}/deps/avs/src/audio_effect"
        "${PROJECT_SOURCE_DIR}/deps/avs/src/audio_io"
        "${PROJECT_SOURCE_DIR}/deps/avs/src/audio_io/mock"
        "${PROJECT_SOURCE_DIR}/deps/avs/src/mediamgr"
        "${PROJECT_SOURCE_DIR}/deps/avs/src/msystem"
        "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re"
        "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re/include"
        "${LIBWEBRTC_INCLUDE_PATH}"
   	    "${LIBWEBRTC_INCLUDE_PATH}/rtc_base"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party"
        "${LIBWEBRTC_INCLUDE_PATH}/third_party/ffmpeg"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/abseil-cpp"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/src"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/celt"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/include"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/silk"
   	    "${LIBWEBRTC_INCLUDE_PATH}/third_party/boringssl/src/include"
        )

# Compile definitions for libwebrtc.
target_compile_definitions(${PROJECT_NAME} PUBLIC
        $<$<NOT:$<PLATFORM_ID:Windows>>:WEBRTC_POSIX>
        $<$<PLATFORM_ID:Windows>:WEBRTC_WIN>
        $<$<PLATFORM_ID:Darwin>:WEBRTC_MAC>
        )

# Public (interface) dependencies.
target_link_libraries(${PROJECT_NAME} PUBLIC
        android
        log
        OpenSLES
        ffmpeg
        avs
        )