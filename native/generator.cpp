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
}
/*
 * Class:     cz_muni_fi_model_Model
 * Method:    cppLoad
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL 
Java_cz_muni_fi_model_Model_cppLoad(
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
        odeModel.RunAbstraction();
        generator = new StateSpaceGenerator(odeModel, true);
       
        //read parameter ranges and add them to java model object
        std::vector<std::pair<double, double> > paramRanges = odeModel.getParamRanges();
        for (int i = 0; i < paramRanges.size(); ++i)
        {
	        auto newRange = jvm.Range.open(paramRanges[i].first, paramRanges[i].second);
	        model.paramList.add(newRange.object());
        }
        //read threashold ranges and add them to javao model object
        for (int i = 0; i < odeModel.getDims(); ++i)
        {
        	std::vector<double> thresholds = odeModel.getThresholdsForVariable(i);
	        auto newRange = jvm.Range.open(0, thresholds.size() - 1);
	        model.varList.add(newRange.object());
        }               
    }
	return;
}

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
} 
/*
 * Class:     cz_muni_fi_distributed_graph_NodeFactory
 * Method:    getNativePredecessors
 * Signature: (Lcz/muni/fi/distributed/graph/Node;Lcz/muni/fi/model/ColorSet;Ljava/util/Map;)Ljava/util/Set;
 */
JNIEXPORT jobject JNICALL 
Java_cz_muni_fi_distributed_graph_NodeFactory_getNativePredecessors(
	JNIEnv * env, jobject jFactory, jintArray jNode, jobject jColorSet, jobject retMap) 
{
	return computeNodes(env, jFactory, jNode, jColorSet, retMap, false);
}

/*
 * Class:     cz_muni_fi_distributed_graph_NodeFactory
 * Method:    getNativeSuccessors
 * Signature: (Lcz/muni/fi/distributed/graph/Node;Lcz/muni/fi/model/ColorSet;Ljava/util/Map;)Ljava/util/Set;
 */
JNIEXPORT jobject JNICALL 
Java_cz_muni_fi_distributed_graph_NodeFactory_getNativeSuccessors(
	JNIEnv * env, jobject jFactory, jintArray jNode, jobject jColorSet, jobject retMap) 
{
	return computeNodes(env, jFactory, jNode, jColorSet, retMap, true);
}

/*
 * Class:     cz_muni_fi_distributed_graph_NodeFactory
 * Method:    getNativeInit
 * Signature: (Ljava/lang/String;IFLjava/util/List;Ljava/util/Map;)Ljava/util/Map;
 */
JNIEXPORT jobject JNICALL Java_cz_muni_fi_distributed_graph_NodeFactory_getNativeInit(
	JNIEnv * env, jobject jFactory, jstring jName, jint jOp, jfloat jTh, jobject jColorList, jobject retMap)
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
}
