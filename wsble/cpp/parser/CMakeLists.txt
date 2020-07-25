cmake_minimum_required(VERSION 3.4.1)

project(
        parser_lib
        VERSION 1.0.0
        DESCRIPTION "Quaternion library"
        LANGUAGES CXX
)

add_library(
        parser
        SHARED

        parser.cpp
)


find_library(
        log-lib
        log
)

target_link_libraries(
        parser
        ${log-lib}
)

set_target_properties(
        parser

        PROPERTIES
        VERSION ${PROJECT_VERSION}
)