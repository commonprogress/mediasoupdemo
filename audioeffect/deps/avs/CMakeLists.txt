cmake_minimum_required(VERSION 3.5)

project(avs LANGUAGES CXX)

# Set version number.
set(avs_VERSION_MAJOR 3)
set(avs_VERSION_MINOR 0)
set(avs_VERSION_PATCH 4)


# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Project configuration.
set(LIBWEBRTC_INCLUDE_PATH "" CACHE STRING "libwebrtc include path")
set(LIBWEBRTC_BINARY_PATH "" CACHE STRING "libwebrtc binary path")


# GCC >= 4.9 is required due to std::regex use.
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS 4.9)
        message(FATAL_ERROR "GCC version must be at least 4.9!")
    endif ()
endif ()


if (${MEDIASOUPCLIENT_LOG_TRACE})
    target_compile_definitions(${PROJECT_NAME}
            PRIVATE MSC_LOG_TRACE=1
            )
endif ()

if (${MEDIASOUPCLIENT_LOG_DEV})
    target_compile_definitions(${PROJECT_NAME}
            PRIVATE MSC_LOG_DEV=1
            )
endif ()

include_directories(include
        src/audio_effect
        src/mediamgr
        src/main
        contrib/re/src/main
        contrib/re/include)
include_directories(../webrtc/include
        ../webrtc/include/rtc_base
        ../webrtc/include/third_party
        ../webrtc/include/third_party/opus
        ../webrtc/include/third_party/opus/src
        ../webrtc/include/third_party/opus/src/include
        ../webrtc/include/third_party/opus/src/silk
        )

add_definitions(-D_LINUX -D_ANDROID)

set(
        SOURCE_FILES
        src/mediamgr/mediamgr.c
        src/mediamgr/mm_platform_dummy.c
        src/mediamgr/sound.c
        src/mediamgr/mm_platform_android.c
        src/audio_effect/aueffect.c
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
        src/log/log.c
)

# Create target.
add_library(${PROJECT_NAME} STATIC ${SOURCE_FILES})


# Add '-whole-archive' to keep symbols from peerconnection_jni.
# https://stackoverflow.com/a/5687860/2085408
SET(webrtc -Wl,--whole-archive ../webrtc/lib/${ANDROID_ABI}/libwebrtc.so -Wl,--no-whole-archive)

target_link_libraries(${PROJECT_NAME} PUBLIC
        ${webrtc}
        )

# Compile definitions for libwebrtc.
target_compile_definitions(${PROJECT_NAME} PUBLIC
        $<$<NOT:$<PLATFORM_ID:Windows>>:WEBRTC_POSIX>
        $<$<PLATFORM_ID:Windows>:WEBRTC_WIN>
        $<$<PLATFORM_ID:Darwin>:WEBRTC_MAC>
        )

install(TARGETS avs DESTINATION lib)