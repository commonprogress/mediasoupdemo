cmake_minimum_required(VERSION 3.5)

project(avs_so LANGUAGES CXX)

# Set version number.
set(avs_so_VERSION_MAJOR 3)
set(avs_so_VERSION_MINOR 0)
set(avs_so_VERSION_PATCH 4)


# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(
        SOURCE_FILES
        src/main/jni/audio_effect.cc
        src/main/jni/media_manager.cc
)

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

add_subdirectory("deps/avs")

# Add some compile flags to our source files.
set_source_files_properties(${SOURCE_FILES}
        PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)

add_definitions(-D_LINUX -D_ANDROID)

include_directories(
        src/main/jni/com_waz_audioeffect_AudioEffect.h
        deps/avs/include
        deps/avs/src/audio_effect
        deps/avs/src/mediamgr
        deps/avs/src/main
        deps/avs/contrib/re/src/main
        deps/avs/contrib/re/include)

include_directories(deps/webrtc/include
        deps/webrtc/include/rtc_base
        deps/webrtc/include/third_party/abseil-cpp
        deps/webrtc/include/third_party/opus
        deps/webrtc/include/third_party/opus/src
        deps/webrtc/include/third_party/opus/src/src
        deps/webrtc/include/third_party/opus/src/celt
        deps/webrtc/include/third_party/opus/src/include
        deps/webrtc/include/third_party/opus/src/silk
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
        re
        avs
        )