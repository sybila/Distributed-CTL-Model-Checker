cp "../ODE Abstraction/build/binaries/libgenerator.jnilib" libgenerator.jnilib
cp "../ODE Abstraction/build/libs/ODE Abstraction-all-1.0.jar" checker.jar
mpjrun.sh -np 1 -jar checker.jar "$1" "$2"