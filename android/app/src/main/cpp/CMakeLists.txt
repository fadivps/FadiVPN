cmake_minimum_required(VERSION 3.22.1)

project("fadi_xray")

add_library(
    xray
    SHARED
    IMPORTED
)

set_target_properties(
    xray
    PROPERTIES
    IMPORTED_LOCATION
    "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libxray.so"
)

add_library(
    fadi_xray
    SHARED
    fadi_xray.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    fadi_xray
    xray
    ${log-lib}
)
