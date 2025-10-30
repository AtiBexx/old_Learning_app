# A szükséges CMake minimális verzióját állítja be.
cmake_minimum_required(VERSION 3.18.1)

# Definiálja a projekt nevét, ami a lefordított könyvtár neve lesz.
# A `libnative-lib.so` nevű fájl fog létrejönni.
project("native-lib")

# Létrehoz egy SHARED (megosztott) könyvtárat a megadott forrásfájlból.
add_library(
        native-lib
        SHARED
        native-lib.cpp)

# Hozzáadja a 'cpp' mappát az include keresési útvonalakhoz.
# Így a fordító megtalálja a 'cpp/nlohmann/Json.hpp'-t.
target_include_directories(native-lib PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Megkeresi a JNI-hoz és a logoláshoz szükséges Android könyvtárakat.
find_library(
        log-lib
        log)

# Hozzálinkeli a szükséges Android könyvtárakat a mi könyvtárunkhoz.
target_link_libraries(
        native-lib
        ${log-lib})
