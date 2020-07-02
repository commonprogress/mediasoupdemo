cmake_minimum_required(VERSION 3.5)

#project(avs LANGUAGES CXX)
project(re)

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

# 设置C语言标准
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_VERBOSE_MAKEFILE ON)

# Project configuration.
set(LIBWEBRTC_INCLUDE_PATH "" CACHE STRING "libwebrtc include path")
set(LIBWEBRTC_BINARY_PATH "" CACHE STRING "libwebrtc binary path")

if (NOT LIBWEBRTC_INCLUDE_PATH)
    message(FATAL_ERROR "LIBWEBRTC_INCLUDE_PATH not provided")
endif ()

if (NOT LIBWEBRTC_BINARY_PATH)
    message(FATAL_ERROR "LIBWEBRTC_BINARY_PATH not provided")
endif ()

message("\n=========== libre Build Configuration ===========\n")
#message(STATUS "MEDIASOUPCLIENT_BUILD_TESTS : " ${MEDIASOUPCLIENT_BUILD_TESTS})
#message(STATUS "MEDIASOUPCLIENT_LOG_TRACE   : " ${MEDIASOUPCLIENT_LOG_TRACE})
#message(STATUS "MEDIASOUPCLIENT_LOG_DEV     : " ${MEDIASOUPCLIENT_LOG_DEV})
message(STATUS "LIBWEBRTC_INCLUDE_PATH      : " ${LIBWEBRTC_INCLUDE_PATH})
message(STATUS "LIBWEBRTC_BINARY_PATH       : " ${LIBWEBRTC_BINARY_PATH})
message(STATUS "PROJECT_SOURCE_DIR      : " ${PROJECT_SOURCE_DIR})
message("")

# GCC >= 4.9 is required due to std::regex use.
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS 4.9)
        message(FATAL_ERROR "GCC version must be at least 4.9!")
    endif ()
endif ()

SET(RE_ROOT ${PROJECT_SOURCE_DIR})
SET(RE_SRC_PREFIX src)

file(GLOB_RECURSE SOURCE_FILES "src/*.c")

# include header files for the IDEs
file(GLOB_RECURSE HEADER_FILES src/*.h include/*.h)

add_library(re STATIC ${SOURCE_FILES} ${HEADER_FILES})

if (MSVC)
    set_source_files_properties(${SOURCE_FILES} ${HEADER_FILES}
            PROPERTIES COMPILE_FLAGS "/W3 /WX")
else ()
    set_source_files_properties(${SOURCE_FILES} ${HEADER_FILES}
            PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic)
endif ()

# Private (implementation) header files.
target_include_directories(${PROJECT_NAME} PRIVATE ${PROJECT_SOURCE_DIR}/include)

# Public (interface) headers from dependencies.
target_include_directories(${PROJECT_NAME} PUBLIC
        ../../../webrtc/src
        ../../../webrtc/src/rtc_base
        ../../../webrtc/src/third_party
        ../../../webrtc/src/third_party/abseil-cpp
        ../../../webrtc/src/third_party/ffmpeg
        ../../../webrtc/src/third_party/opus/src
        ../../../webrtc/src/third_party/opus/src/src
        ../../../webrtc/src/third_party/opus/src/celt
        ../../../webrtc/src/third_party/opus/src/include
        ../../../webrtc/src/third_party/opus/src/silk
        ../../../webrtc/src/third_party/boringssl/src/include
        )

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libffmpeg.so)

# Public (interface) dependencies.
if ("${ANDROID_ABI}" STREQUAL "")
    message("\n=========== libre 222222 ===========\n")
    target_link_libraries(${PROJECT_NAME} PUBLIC
            ffmpeg
            ${LIBWEBRTC_BINARY_PATH}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX}
            )
else ()
    message("\n=========== libre 1111111 ===========\n")
    message(STATUS "ANDROID_ABI      : " ${ANDROID_ABI})
    # Add '-whole-archive' to keep symbols from peerconnection_jni.
    # https://stackoverflow.com/a/5687860/2085408
    #    SET(re -Wl,--whole-archive ${PROJECT_SOURCE_DIR}/contrib/re/lib/${ANDROID_ABI}/libre${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
    SET(webrtc -Wl,--whole-archive ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI}/libwebrtc${CMAKE_STATIC_LIBRARY_SUFFIX} -Wl,--no-whole-archive)
    target_link_libraries(${PROJECT_NAME} PUBLIC
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

install(TARGETS re DESTINATION lib)
