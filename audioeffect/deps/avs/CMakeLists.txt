cmake_minimum_required(VERSION 3.5)

project(avs LANGUAGES CXX)

# Set version number.
set(avs_VERSION_MAJOR 3)
set(avs_VERSION_MINOR 0)
set(avs_VERSION_PATCH 8)

# Configure a header file to pass the version.
#configure_file (
#        "${PROJECT_SOURCE_DIR}/version.hpp.in"
#        "${PROJECT_SOURCE_DIR}/include/version.hpp"
#)

# C++ standard requirements.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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
message(STATUS "PROJECT_NAME       : " ${PROJECT_NAME})
message(STATUS "PROJECT_SOURCE_DIR       : " ${PROJECT_SOURCE_DIR})
message("")

# GCC >= 4.9 is required due to std::regex use.
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS 4.9)
        message(FATAL_ERROR "GCC version must be at least 4.9!")
    endif ()
endif ()

#if (${MEDIASOUPCLIENT_BUILD_TESTS})
#    add_subdirectory(test)
#endif()

#set(HEADER_FILES
#        include/mediasoupclient.hpp
#        )
set(
        SOURCE_FILES
        src/mediamgr/mediamgr.cpp
        src/mediamgr/mm_platform_dummy.cpp
        src/mediamgr/sound.cpp
        src/mediamgr/mm_platform_android.cpp
        src/audio_effect/aueffect.cpp
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
        src/log/log.cpp
)

# Create target.
add_library(${PROJECT_NAME} STATIC ${SOURCE_FILES})

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
#add_subdirectory(deps/libsdptransform "${CMAKE_CURRENT_BINARY_DIR}/libsdptransform")

# Add some compile flags to our source files.
if (MSVC)
    set_source_files_properties(${SOURCE_FILES}
            PROPERTIES COMPILE_FLAGS "/W3 /WX")
else ()
    set_source_files_properties(${SOURCE_FILES}
            PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)
endif ()

# Private (implementation) header files.
target_include_directories(${PROJECT_NAME} PRIVATE ${PROJECT_SOURCE_DIR}/include)

# Public (interface) headers from dependencies.
target_include_directories(${PROJECT_NAME} PUBLIC
        include
        src/audio_effect
        src/mediamgr
        src/main
        contrib
        contrib/re
        contrib/re/include
        ../webrtc/src
        ../webrtc/src/rtc_base
        ../webrtc/src/third_party
        ../webrtc/src/third_party/abseil-cpp
        ../webrtc/src/third_party/ffmpeg
        ../webrtc/src/third_party/opus/src
        ../webrtc/src/third_party/opus/src/src
        ../webrtc/src/third_party/opus/src/celt
        ../webrtc/src/third_party/opus/src/include
        ../webrtc/src/third_party/opus/src/silk
        )

#target_include_directories(${PROJECT_NAME} PUBLIC
#        "${PROJECT_SOURCE_DIR}/include"
#        "${PROJECT_SOURCE_DIR}/src/audio_effect"
#        "${PROJECT_SOURCE_DIR}/src/mediamgr"
#        "${PROJECT_SOURCE_DIR}/src/main"
#        "${PROJECT_SOURCE_DIR}/contrib/re/src/main"
#        "${PROJECT_SOURCE_DIR}/contrib/re/include"
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

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libffmpeg.so)

add_library(re STATIC IMPORTED)
set_target_properties(re
        PROPERTIES IMPORTED_LOCATION
        ${PROJECT_SOURCE_DIR}/contrib/re/libre.a)

# Public (interface) dependencies.
if ("${ANDROID_ABI}" STREQUAL "")
    target_link_libraries(${PROJECT_NAME} PUBLIC
#            sdptransform
            ffmpeg
            re
            ${LIBWEBRTC_BINARY_PATH}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX}
            )
else ()
    # Add '-whole-archive' to keep symbols from peerconnection_jni.
    # https://stackoverflow.com/a/5687860/2085408
    SET(webrtc -Wl,--whole-archive ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
    target_link_libraries(${PROJECT_NAME} PUBLIC
#            sdptransform
            ffmpeg
            re
            ${webrtc}
            )
endif ()

# Compile definitions for libwebrtc.
target_compile_definitions(${PROJECT_NAME} PUBLIC
        $<$<NOT:$<PLATFORM_ID:Windows>>:WEBRTC_POSIX>
        $<$<PLATFORM_ID:Windows>:WEBRTC_WIN>
        $<$<PLATFORM_ID:Darwin>:WEBRTC_MAC>
        )

install(TARGETS avs DESTINATION lib)
#install(FILES ${HEADER_FILES} DESTINATION include/avs)