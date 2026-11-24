cmake_minimum_required(VERSION 3.22.1)
project(retropocket_jni)

add_library(retropocket_jni SHARED
    retropocket_jni.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_include_directories(retropocket_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(retropocket_jni ${log-lib} ${android-lib} dl)
