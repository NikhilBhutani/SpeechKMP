cmake_minimum_required(VERSION 3.22)
project(speech_jni)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ═══════════════════════════════════════════════════════════════
#                        PATHS
# ═══════════════════════════════════════════════════════════════

set(WHISPER_DIR "${CMAKE_SOURCE_DIR}/../../../../../whisper.cpp")
set(PIPER_DIR "${CMAKE_SOURCE_DIR}/../../../../../piper")
set(ONNX_DIR "${CMAKE_SOURCE_DIR}/../../../../../onnxruntime")
set(ESPEAK_DIR "${CMAKE_SOURCE_DIR}/../../../../../espeak-ng")

# ═══════════════════════════════════════════════════════════════
#                      WHISPER.CPP
# ═══════════════════════════════════════════════════════════════

# Disable unnecessary features
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)  # No Metal on Android

add_subdirectory(${WHISPER_DIR} whisper-build EXCLUDE_FROM_ALL)

# ═══════════════════════════════════════════════════════════════
#                         PIPER
# ═══════════════════════════════════════════════════════════════

# ONNX Runtime (pre-built for Android)
if(ANDROID)
    set(ONNX_RUNTIME_LIB "${ONNX_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so")
else()
    # Desktop - find system ONNX Runtime or use bundled
    find_library(ONNX_RUNTIME_LIB onnxruntime HINTS ${ONNX_DIR}/lib)
endif()

# espeak-ng (build as static library)
set(USE_ASYNC OFF CACHE BOOL "" FORCE)
set(USE_MBROLA OFF CACHE BOOL "" FORCE)
set(USE_LIBPCAUDIO OFF CACHE BOOL "" FORCE)
set(USE_KLATT OFF CACHE BOOL "" FORCE)
set(USE_SPEECHPLAYER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

if(EXISTS ${ESPEAK_DIR}/CMakeLists.txt)
    add_subdirectory(${ESPEAK_DIR} espeak-build EXCLUDE_FROM_ALL)
endif()

# Piper sources
if(EXISTS ${PIPER_DIR}/src/cpp/piper.cpp)
    add_library(piper STATIC
        ${PIPER_DIR}/src/cpp/piper.cpp
        ${PIPER_DIR}/src/cpp/phonemize.cpp
        ${PIPER_DIR}/src/cpp/phoneme_ids.cpp
    )
    target_include_directories(piper PRIVATE
        ${PIPER_DIR}/src/cpp
        ${ONNX_DIR}/include
        ${ESPEAK_DIR}/src/include
    )
    if(TARGET espeak-ng)
        target_link_libraries(piper espeak-ng)
    endif()
    if(ONNX_RUNTIME_LIB)
        target_link_libraries(piper ${ONNX_RUNTIME_LIB})
    endif()
endif()

# ═══════════════════════════════════════════════════════════════
#                      JNI LIBRARY
# ═══════════════════════════════════════════════════════════════

if(ANDROID)
    find_library(log-lib log)
endif()

add_library(speech_jni SHARED
    whisper_jni.cpp
    piper_jni.cpp
)

target_include_directories(speech_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}
)

if(EXISTS ${PIPER_DIR}/src/cpp)
    target_include_directories(speech_jni PRIVATE
        ${PIPER_DIR}/src/cpp
        ${ONNX_DIR}/include
    )
endif()

# Link libraries
target_link_libraries(speech_jni whisper)

if(TARGET piper)
    target_link_libraries(speech_jni piper)
endif()

if(ANDROID)
    target_link_libraries(speech_jni ${log-lib})
endif()

# ═══════════════════════════════════════════════════════════════
#                      COMPILE FLAGS
# ═══════════════════════════════════════════════════════════════

if(ANDROID)
    target_compile_options(speech_jni PRIVATE
        -fPIC
        -fvisibility=hidden
        -O3
    )
else()
    target_compile_options(speech_jni PRIVATE
        -fPIC
        -O3
    )
endif()
