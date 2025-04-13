cmake_minimum_required(VERSION 3.22.1)
project(aosp-triggers-daemon LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(${CMAKE_PROJECT_NAME})

file(GLOB_RECURSE PROJECT_HEADERS ${CMAKE_CURRENT_SOURCE_DIR}/src/*.h)
file(GLOB_RECURSE PROJECT_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp)

target_sources(${PROJECT_NAME} PRIVATE
        ${PROJECT_HEADERS}
        ${PROJECT_SOURCES}
)
target_include_directories(${PROJECT_NAME} PRIVATE src/)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)