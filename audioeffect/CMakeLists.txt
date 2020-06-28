cmake_minimum_required(VERSION 3.5)

project(avs_so LANGUAGES CXX)

# Set version number.
set(avs_so_VERSION_MAJOR 3)
set(avs_so_VERSION_MINOR 0)
set(avs_so_VERSION_PATCH 8)


# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)


message("\n=========== libaudioeffect Build Configuration ===========\n")
#message(STATUS "MEDIASOUPCLIENT_BUILD_TESTS : " ${MEDIASOUPCLIENT_BUILD_TESTS})
#message(STATUS "MEDIASOUPCLIENT_LOG_TRACE   : " ${MEDIASOUPCLIENT_LOG_TRACE})
#message(STATUS "MEDIASOUPCLIENT_LOG_DEV     : " ${MEDIASOUPCLIENT_LOG_DEV})
message(STATUS "LIBWEBRTC_INCLUDE_PATH      : " ${LIBWEBRTC_INCLUDE_PATH})
message(STATUS "LIBWEBRTC_BINARY_PATH       : " ${LIBWEBRTC_BINARY_PATH})
message(STATUS "PROJECT_NAME       : " ${PROJECT_NAME})
message(STATUS "PROJECT_SOURCE_DIR       : " ${PROJECT_SOURCE_DIR})
message(STATUS "avs_so_SOURCE_DIR       : " ${avs_so_SOURCE_DIR})
message("")

set(
        SOURCE_DIR
        ${avs_so_SOURCE_DIR}/src/main/jni/
)

set(
        SOURCE_FILES
        ${SOURCE_DIR}/audio_effect.cc
        ${SOURCE_DIR}/media_manager.cc
)

# TODO(haiyang wu): add condition
#list(APPEND SOURCE_FILES ${PROJECT_SOURCE_DIR}/src/androidTest/jni/parameters_jni.cpp)
#list(APPEND SOURCE_FILES ${PROJECT_SOURCE_DIR}/deps/libmediasoupclient/test/src/fakeParameters.cpp)

# Create target.
add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

if(${MEDIASOUPCLIENT_LOG_TRACE})
    target_compile_definitions(${PROJECT_NAME}
            PRIVATE MSC_LOG_TRACE=1
            )
endif()

if(${MEDIASOUPCLIENT_LOG_DEV})
    target_compile_definitions(${PROJECT_NAME}
            PRIVATE MSC_LOG_DEV=1
            )
endif()

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

#include_directories(SYSTEM "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re/src/main")
#include_directories(SYSTEM "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re/include")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/rtc_base")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/abseil-cpp")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/src")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/celt")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/include")
#include_directories(SYSTEM "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/silk")

include_directories(SYSTEM /deps/avs/contrib)
include_directories(SYSTEM /deps/avs/contrib/re)
include_directories(SYSTEM /deps/avs/contrib/re/include)
include_directories(SYSTEM /deps/webrtc/src/rtc_base)
include_directories(SYSTEM /deps/webrtc/src/third_party)
include_directories(SYSTEM /deps/webrtc/src/third_party/abseil-cpp)
include_directories(SYSTEM /deps/webrtc/src/third_party/ffmpeg)
include_directories(SYSTEM /deps/webrtc/src/third_party/opus/src)
include_directories(SYSTEM /deps/webrtc/src/third_party/opus/src/src)
include_directories(SYSTEM /deps/webrtc/src/third_party/opus/src/celt)
include_directories(SYSTEM /deps/webrtc/src/third_party/opus/src/include)
include_directories(SYSTEM /deps/webrtc/src/third_party/opus/src/silk)

# Private (implementation) header files.
#target_include_directories(${PROJECT_NAME} PUBLIC
#        "${SOURCE_DIR}/"
#        "${PROJECT_SOURCE_DIR}/deps/avs/include"
#        "${PROJECT_SOURCE_DIR}/deps/avs/src/audio_effect"
#        "${PROJECT_SOURCE_DIR}/deps/avs/src/mediamgr"
#        "${PROJECT_SOURCE_DIR}/deps/avs/src/main"
#        "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re/src/main"
#        "${PROJECT_SOURCE_DIR}/deps/avs/contrib/re/include"
#        "${LIBWEBRTC_INCLUDE_PATH}"
#        "${LIBWEBRTC_INCLUDE_PATH}/rtc_base"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/abseil-cpp"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/src"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/celt"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/include"
#        "${LIBWEBRTC_INCLUDE_PATH}/third_party/opus/src/silk"
#        )

target_include_directories(${PROJECT_NAME} PUBLIC
        "${SOURCE_DIR}/"
        /deps/avs/include
        /deps/avs/src/audio_effect
        /deps/avs/src/mediamgr
        /deps/avs/contrib
        /deps/avs/contrib/re
        /deps/avs/contrib/re/include
        /deps/webrtc/src
        /deps/webrtc/src/rtc_base
        /deps/webrtc/src/third_party
        /deps/webrtc/src/third_party/abseil-cpp
        /deps/webrtc/src/third_party/ffmpeg
        /deps/webrtc/src/third_party/opus/src
        /deps/webrtc/src/third_party/opus/src/src
        /deps/webrtc/src/third_party/opus/src/celt
        /deps/webrtc/src/third_party/opus/src/include
        /deps/webrtc/src/third_party/opus/src/silk
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