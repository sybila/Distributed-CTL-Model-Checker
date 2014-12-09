GCC=g++-4.9
JDK=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home

#Run: mpjrun.sh -np 4 -jar ../out/artifacts/Parametrized_CTL_Model_Checker_jar/Parametrized\ CTL\ Model\ Checker.jar 'E( {pRB < 5.0} U EX {pRB < 10.0} )'
all: generator

clean:
	@rm -f generator.jnilib
	@rm -f generator.o
	@rm -f jni_include/NodeFactory.h
	@rm -f jni_include/Model.h

generator.o: generator.cpp jni_include/NodeFactory.h jni_include/Model.h
	@echo "Compiling..."
	@$(GCC) -std=c++11 -I $(JDK)/include/ -I $(JDK)/include/darwin/ -c generator.cpp 

generator: generator.o jni_include/NodeFactory.h jni_include/Model.h
	@echo "Linking..."
	@$(GCC) -dynamiclib -o libgenerator.jnilib generator.o
	@cp libgenerator.jnilib ../nlib/libgenerator.jnilib 

jni_include/NodeFactory.h:
	@mkdir -p jni_include
	@javah -o jni_include/NodeFactory.h -classpath ../out/artifacts/Parametrized_CTL_Model_Checker_jar/* cz.muni.fi.distributed.graph.NodeFactory

jni_include/Model.h:
	@mkdir -p jni_include
	@javah -o jni_include/Model.h -classpath ../out/artifacts/Parametrized_CTL_Model_Checker_jar/* cz.muni.fi.model.Model

run:
	mpjrun.sh -np 4 -Xmx6g -jar ../out/artifacts/Parametrized_CTL_Model_Checker_jar/Parametrized\ CTL\ Model\ Checker.jar examples/tcbb1param.bio 'E( {pRB < 5.0} U EX {pRB < 10.0} )'