#include "jni_include/Model.h"
#include "jni_include/NodeFactory.h"
#include <iostream>
#include <vector>
#include "data_model/Model.h"
#include "parser/parse.cc"
#include "scanner/lex.cc"
#include "jni_helper.h"
#include "ssg.h"

StateSpaceGenerator * generator;
Model<double> odeModel;

/*
void saveStatesToMap(vector<State>& data, JVM::MapClass::Instance map, JVM::NodeFactoryClass::Instance factory, JVM jvm) {
	if (data.size() != 0) {
		//create int java array and it's c++ mapping
		int dims = data[0].getCoors().size();
		jint * array = new jint[dims];
		jintArray coordinates = jvm.getEnv()->NewIntArray(dims);		
		for (int i = 0; i < data.size(); i++) {
			State state = data[i];
			//convert state coordinates to java array
			for (int j = 0; j < dims; j++) {
				array[j] = state.getCoors()[j]; 
			}
			//notify JVM about value update
			jvm.getEnv()->SetIntArrayRegion(coordinates, 0, dims, array);
			//get node object from factory
			auto node = factory.getNode(coordinates);
			//convert color list ro java ranges
			auto colors = JVM::ColorSetClass::Instance(state.getColors().getParamSpace(), &(jvm.ColorSet));
			//save to results
			map.put(node.object(), colors.object());
		}	
		delete array;
	}
}*/

/*
 * Class:     cz_muni_fi_ode_OdeModel
 * Method:    cppLoad
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL 
Java_cz_muni_fi_ode_OdeModel_cppLoad(
	JNIEnv * env, jobject jModel, jstring jFilename)
{
	JVM jvm(env);
	//create c++ wrappers around java arguments
	auto model = JVM::ModelClass::Instance(jModel, &(jvm.Model));
    std::ifstream modelfile2 (string(env->GetStringUTFChars(jFilename, 0)));

    if (modelfile2.is_open()) {
    	//load model and create state space generator
        Parser parser(modelfile2);
        parser.parse();
        odeModel = parser.returnStorage();
        odeModel.RunAbstraction(false); // TODO: set false as input parameter finally
        generator = new StateSpaceGenerator(odeModel, true);

/*

				std::vector<std::list<std::pair<double, double> > > paramSpace;
        		std::list<std::pair<double, double> > param;
        		param.push_back(std::pair<double, double>(odeModel.getParamRanges()[0]));
        		paramSpace.push_back(param);

                for (long unsigned int i=0; i<odeModel.getThresholdsForVariable(0).size()-1; i++) {
                	State s(1, {i}, paramSpace);
                	vector<State> data = generator->getSucc(s);
                	std::cout << "Succ for: " << s;
                	for (int i = 0; i < data.size(); ++i)
                	{
                		std::cout << data[i];
                	}
                }*/

        //read parameter ranges and add them to java model object
        //std::cout << "Copying parameter ranges" << std::endl;
        std::vector<std::pair<double, double> > paramRanges = odeModel.getParamRanges();
        for (int i = 0; i < paramRanges.size(); ++i)
        {
        	//closed, because we also accept singular points
	        auto newRange = jvm.Range.closedDouble(paramRanges[i].first, paramRanges[i].second);
	        model.paramList.add(newRange.object());
        }
        //read threashold ranges and add them to java model object
        //std::cout << "Copying threshold ranges" << std::endl;
        for (int i = 0; i < odeModel.getDims(); ++i)
        {
        	//write threshold range
        	std::vector<double> thresholds = odeModel.getThresholdsForVariable(i);
	        auto newRange = jvm.Range.closedInt(0, thresholds.size() - 1);
	        model.varList.add(newRange.object());
	        //std::cout << "Threshold range written" << std::endl;
	        //init name->index function
	        model.variableOrder.add(env->NewStringUTF(odeModel.getVariable(i).c_str()));
	        //std::cout << "Variable order written" << std::endl;
	        //copy thresholds
        	auto tList = jvm.List.create();
        	for (int j = 0; j < thresholds.size(); j++) {
        		//std::cout << "T "<< i <<j << " " << thresholds[j] <<std::endl;
        		tList.add(jvm.Double.valueOf(thresholds[j]).object());
        		//std::cout << "Threshold written" << std::endl;
        	}
        	model.thresholds.add(tList.object());
        	//copy equation
        	auto sumMembers = odeModel.getEquationForVariable(i);
        	auto sList = jvm.List.create();
        	for (int j = 0; j < sumMembers.size(); j++) {
				//std::cout << "S "<<i<<j<<" "<<sumMembers[j]<<std::endl;
        		sList.add(jvm.SumMember.create(sumMembers[j]).object());
        		//std::cout << "Summember written" << std::endl;
        	}
        	model.equations.add(sList.object());
        	//std::cout << "Equation written" << std::endl;
		}

	/*	std::vector<std::pair<double, double> > borders;
		for (int i = 0; i < odeModel.getDims(); ++i)
		{
			borders.push_back(pair<double, double>(0, odeModel.getThresholdsForVariable(i).size() - 1));
		}
		vector<State> data = generator->initAP(
			odeModel.getVariable(0),
			Operators::LSEQ,
			(double) odeModel.getThresholdsForVariable(0).back(),
			borders
		);
		int s = 0;
		for (int l = 0; l < data.size(); l++) {
			vector<State> pred = generator->getPred(data[l]);
			s += pred.size();
		}
		std::cout << "Native s: " << s << " " << data.size() << std::endl;*/
    }
  //  std::cout << "Returning" << std::endl;
	return;
}
/*
jobject computeNodes(
	JNIEnv * env, jobject jFactory, jintArray jNode, jobject jColorSet, jobject retMap, bool succ) {
	JVM jvm(env);
	//create c++ wrapers around java arguments
	auto factory = JVM::NodeFactoryClass::Instance(jFactory, &(jvm.NodeFactory));
	auto results = JVM::MapClass::Instance(retMap, &(jvm.Map));
	auto colorSet = JVM::ColorSetClass::Instance(jColorSet, &(jvm.ColorSet));
	//translate node coordinates to c++ array
	std::vector<size_t> coordinates;
	jsize dims = env->GetArrayLength(jNode);
	jint * coordArray = env->GetIntArrayElements(jNode, 0);
	for (int i = 0; i < dims; ++i)
	{
		coordinates.push_back(coordArray[i]);
	}
	if (succ) {
		vector<State> data = generator->getSucc(State(dims, coordinates, ParameterSpace(colorSet.toParamSpace())));	
		saveStatesToMap(data, results, factory, jvm);
	} else {
		vector<State> data = generator->getPred(State(dims, coordinates, ParameterSpace(colorSet.toParamSpace())));	
		saveStatesToMap(data, results, factory, jvm);
	}
	return results.object();
} */
/*
 * Class:     cz_muni_fi_ode_NodeFactory
 * Method:    getNativePredecessors
 * Signature: ([ILcz/muni/fi/modelchecker/graph/ColorSet;Ljava/util/Map;)Ljava/util/Map;
 */
/*JNIEXPORT jobject JNICALL Java_cz_muni_fi_ode_NodeFactory_getNativePredecessors(
	JNIEnv * env, jobject jFactory, jintArray jNode, jobject jColorSet, jobject retMap)
{
	return computeNodes(env, jFactory, jNode, jColorSet, retMap, false);
}*/

/*
 * Class:     cz_muni_fi_ode_NodeFactory
 * Method:    getNativeSuccessors
 * Signature: ([ILcz/muni/fi/modelchecker/graph/ColorSet;Ljava/util/Map;)Ljava/util/Map;
 */
/*JNIEXPORT jobject JNICALL Java_cz_muni_fi_ode_NodeFactory_getNativeSuccessors(
	JNIEnv * env, jobject jFactory, jintArray jNode, jobject jColorSet, jobject retMap)
{
	return computeNodes(env, jFactory, jNode, jColorSet, retMap, true);
}*/

/*
 * Class:     cz_muni_fi_ode_NodeFactory
 * Method:    getNativeInit
 * Signature: (Ljava/lang/String;IDLjava/util/List;Ljava/util/Map;)Ljava/util/Map;
 */
/*JNIEXPORT jobject JNICALL Java_cz_muni_fi_ode_NodeFactory_getNativeInit(
	JNIEnv * env, jobject jFactory, jstring jName, jint jOp, jdouble jTh, jobject jColorList, jobject retMap)
{
	JVM jvm(env);
	//create c++ wrappers around java arguments
	auto factory = JVM::NodeFactoryClass::Instance(jFactory, &(jvm.NodeFactory));
	auto results = JVM::MapClass::Instance(retMap, &(jvm.Map));
	auto colorList = JVM::ListClass::Instance(jColorList, &(jvm.List));
	//transform java range list into c++ pair vector
	std::vector<std::pair<double, double> > borders;
	int paramsCount = colorList.size();
	for (int i = 0; i < paramsCount; ++i)
	{
		auto range = JVM::RangeClass::Instance(colorList.get(i), &(jvm.Range));
		borders.push_back(pair<double, double>(range.lowerEndPoint().doubleValue(), range.upperEndPoint().doubleValue()));
	}
	//compute initial states
	vector<State> data = generator->initAP(
		std::string(env->GetStringUTFChars(jName, 0)), 
		Operators(jOp), 
		(double) jTh, 
		borders
	);
	saveStatesToMap(data, results, factory, jvm);
	return results.object();
}*/



/*
 * Class:     cz_muni_fi_ode_NodeFactory
 * Method:    cacheAllNodes
 * Signature: (Ljava/util/List;)V
 */
/*JNIEXPORT void JNICALL Java_cz_muni_fi_ode_NodeFactory_cacheAllNodes(
	JNIEnv * env, jobject jFactory, jobject jColorList)
 {
	JVM jvm(env);
	//create c++ wrappers around java arguments
	auto factory = JVM::NodeFactoryClass::Instance(jFactory, &(jvm.NodeFactory));
	auto colorList = JVM::ListClass::Instance(jColorList, &(jvm.List));
	//transform java range list into c++ pair vector
	std::vector<std::pair<double, double> > borders;
	int paramsCount = colorList.size();
	for (int i = 0; i < paramsCount; ++i)
	{
		auto range = JVM::RangeClass::Instance(colorList.get(i), &(jvm.Range));
		borders.push_back(pair<double, double>(range.lowerEndPoint().doubleValue(), range.upperEndPoint().doubleValue()));
	}
	//compute initial states
	vector<State> data = generator->initAP(
		odeModel.getVariable(0), 
		Operators::LSEQ, 
		(double) odeModel.getThresholdsForVariable(0).back(), 
		borders
	);	
	if (data.size() != 0) {
		//create int java array and it's c++ mapping
		int dims = data[0].getCoors().size();
		jint * array = new jint[dims];
		jintArray coordinates = jvm.getEnv()->NewIntArray(dims);		
		for (int i = 0; i < data.size(); i++) {
			State state = data[i];
			//convert state coordinates to java array
			for (int j = 0; j < dims; j++) {
				array[j] = state.getCoors()[j]; 
			}
			//notify JVM about value update
			jvm.getEnv()->SetIntArrayRegion(coordinates, 0, dims, array);
			//get node object from factory
			auto node = factory.getNode(coordinates);			
		}	
		delete array;
	}
}*/
