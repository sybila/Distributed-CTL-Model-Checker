# Distributed Coloured CTL Model Checker

## Installation 

1. You will need the (latest) [Gradle](https://gradle.org/)
  * Download latest gradle, extract it and place the bin folder into your PATH
2. Download source from master branch
3. In the build.gradle (In the root folder of the project), fill in:
  * The compiler - this will be used to compile C++ code and should support C++11
  * Path to Java installation - the folder which contains bin/java - If you are not sure, use 'which java'
  * Target OS - Linux/OS X (relevant for the C++ compilation)
4. If you want to use ODE module:
  * In ODE Abstraction folder, run 
    * gradle generateIncludeFiles 
    (This will generate the JNI include files. Unless you want to mess with itnerface of ODE Model class, 
    you don't need to run it again.)
    * gradle buildGenerator
    (This will compile the C++ ODE generator. You need to run this if you modify any C++ code.)
  * Finally, in Frontend folder, run gradle odeJar (You need to run this if you modify any Java code)
  * Your jar should be in Frontend/build/libs/Frontend-ode-version.jar 
5. If you want to use Thomas Network Module:
  * You need to have Boost installed
  * In Thomas Network Abstraction, run
    * gradle generateIncludeFiles
    * gradle buildGenerator
  * In Frontend folder, run thomasJar
  * Your jar should be in Frontend/build/libs/Frontend-thomas-version.jar 
6. In Frontend folder, you can also use:
  * gradle patternJar to build an ODE model sink/source pattern analyzer
  * gradle transJar to build an ODE model to transition system converter (printed in json)
  
Note that the first run of gradle can take longer, since all dependencies have to be downloaded.

Runtime: You need to have [MPJ](http://mpj-express.org/) installed. There is a tutorial on their website, or you
can download it and use following script to prepare it every time you run the model checker:

```
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export MPJ_HOME=$DIR/mpj
export PATH=$MPJ_HOME/bin:$PATH
mpjrun.sh -np $4 -Xmx10g -jar checker.jar "$3" "$1" "$2"
```
