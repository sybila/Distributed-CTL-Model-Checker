#include <iostream>
#include <vector>

#include "auxiliary/time_manager.hpp"
#include "auxiliary/output_streamer.hpp"
#include "auxiliary/user_options.hpp"
#include "parsing/argument_parser.hpp"
#include "parsing/data_parser.hpp"
#include "parsing/parsing_manager.hpp"
#include "parsing/explicit_filter.hpp"
#include "construction/construction_manager.hpp"
#include "construction/product_builder.hpp"
#include "synthesis/synthesis_manager.hpp"

#include "jni_include/Model.h"
#include "jni_helper.h"

using namespace std;


void prepare(JVM & jvm, vector<short> source, jint * dest, jintArray array) {
	for (int i = 0; i < source.size(); ++i)
	{
		dest[i] = source[i];
	}
	//notify JVM about value update
	jvm.getEnv()->SetIntArrayRegion(array, 0, source.size(), dest);
}

/*
 * Class:     cz_muni_fi_thomas_NetworkModel
 * Method:    loadNative
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL 
Java_cz_muni_fi_thomas_NetworkModel_loadNative(
	JNIEnv * env, jobject jModel, jstring jFilename) 
{
	JVM jvm(env);
	auto factory = JVM::NodeFactoryClass::Instance(jModel, &(jvm.NodeFactory));
	
	string filename = string(env->GetStringUTFChars(jFilename, 0));

	Kinetics kinetics;
	UnparametrizedStructure structure;

	Model model = ParsingManager::parseModelPath(filename);
	kinetics = ConstructionManager::computeKinetics(model, PropertyAutomaton());
	kinetics.printMe();
	structure = ConstructionManager::computeStructure(model, kinetics);

	//count parameters
	vector<int> subspaceSizes;
	int total = 0;
	for (int i = 0; i < kinetics.species.size(); ++i)
	{
		factory.variableOrdering.add(env->NewStringUTF(kinetics.species[i].name.c_str()));
		total += kinetics.species[i].col_count;
	}
	for (int i = kinetics.species.size() - 1; i >= 0; --i)
	{
		subspaceSizes.push_back(kinetics.species[i].col_count);
	}

	factory.setParamSpaceWidth(total);

	int dims = structure.getStateLevels(0).size();
	jint * temp = new jint[dims];
	jintArray levelsArray = jvm.getEnv()->NewIntArray(dims);		
	for (int i = 0; i < structure.getStateCount(); ++i)
	{
		vector<short> levels = structure.getStateLevels(i);
		prepare(jvm, levels, temp, levelsArray);
		auto node = factory.getNode(levelsArray);
		for (int j = 0; j < structure.getTransitionCount(i); ++j)
		{
			TransConst transition = structure.getTransitionConst(i, j);
			int offset = 0;
			int step_size = 1;
			for (int k = 0; k < subspaceSizes.size() && step_size != transition.step_size; ++k)
			{
				offset += subspaceSizes[k];
				step_size *= subspaceSizes[k];
			}
			auto colors = JVM::ColorSetClass::Instance(transition, offset, total, &(jvm.ColorSet));
			std::vector<short> targetLevels = structure.getStateLevels(structure.getTargetID(i, j));
			prepare(jvm, targetLevels, temp, levelsArray);
			auto target = factory.getNode(levelsArray);
			node.addSuccessor(target, colors);
			target.addPredecessor(node, colors);
		}
	}
	delete temp;

}
