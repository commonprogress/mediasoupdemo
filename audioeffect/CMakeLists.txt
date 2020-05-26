cmake_minimum_required(VERSION 3.5)

project(audioeffect LANGUAGES CXX)

# Set version number.
set(audioeffect_VERSION_MAJOR 3)
set(audioeffect_VERSION_MINOR 0)
set(audioeffect_VERSION_PATCH 4)


# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)


include_directories(src/main/jni/avs/include)


add_definitions(-D_LINUX -D_ANDROID)

add_library(audioeffect
        SHARED
        src/main/jni/audio_effect.cc
        src/main/jni/media_manager.cc
        src/main/jni/avs/src/mediamgr/mediamgr.c
        src/main/jni/avs/src/mediamgr/sound.c
        src/main/jni/avs/src/mediamgr/mm_platform_android.c
        src/main/jni/avs/src/audio_effect/aueffect.c
        src/main/jni/avs/src/audio_effect/chorus.cpp
        src/main/jni/avs/src/audio_effect/reverb.cpp
        src/main/jni/avs/src/audio_effect/pitch_shift.cpp
        src/main/jni/avs/src/audio_effect/pace_shift.cpp
        src/main/jni/avs/src/audio_effect/vocoder.cpp
        src/main/jni/avs/src/audio_effect/auto_tune.cpp
        src/main/jni/avs/src/audio_effect/harmonizer.cpp
        src/main/jni/avs/src/audio_effect/normalizer.cpp
        src/main/jni/avs/src/audio_effect/pitch_cycler.cpp
        src/main/jni/avs/src/audio_effect/pass_through.cpp
        src/main/jni/avs/src/audio_effect/find_pitch_lags.cpp
        src/main/jni/avs/src/audio_effect/time_scale.cpp
        src/main/jni/avs/src/audio_effect/biquad.cpp
        src/main/jni/avs/src/audio_effect/wav_interface.cpp
        src/main/jni/avs/src/audio_effect/pcm_interface.cpp
        )


#set(
#        SOURCE_DIR
#        ${maudioeffect_SOURCE_DIR}/src/main/jni/
#)
#
#set(
#        SOURCE_FILES
#        ${SOURCE_DIR}/audio_effect.cc
#        ${SOURCE_DIR}/media_manager.cc
#)
#
## Source Dependencies.
#add_subdirectory("src/main/jni/avs")
#
## Add some compile flags to our source files.
#set_source_files_properties(${SOURCE_FILES}
#        PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)
#
#
## Private (implementation) header files.
#target_include_directories(${PROJECT_NAME} PUBLIC
#        "${SOURCE_DIR}avs/include"
#        "${SOURCE_DIR}avs/src"
#        )
#
## Public (interface) dependencies.
#target_link_libraries(${PROJECT_NAME} PUBLIC
#        android
#        log
#        OpenSLES
#        )