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
 * Class:     cz_muni_fi_thomas_NativeModel
 * Method:    loadNative
 * Signature: (Ljava/lang/String;Lcz/muni/fi/thomas/NetworkModel;)V
 */
JNIEXPORT void JNICALL Java_cz_muni_fi_thomas_NativeModel_loadNative(
    JNIEnv * env, jobject jModel, jstring jFilename, jobject jFactory)
{
	JVM jvm(env);
	auto factory = JVM::NodeFactoryClass::Instance(jFactory, &(jvm.NodeFactory));
	auto nativeModel = JVM::ModelClass::Instance(jModel, &(jvm.Model));


    /** load model from file **/

	string filename = string(env->GetStringUTFChars(jFilename, 0));

	Kinetics kinetics;
	UnparametrizedStructure structure;

	Model model = ParsingManager::parseModelPath(filename);
	kinetics = ConstructionManager::computeKinetics(model, PropertyAutomaton());
	//kinetics.printMe();

	/** copy kinetics data into java model **/
	cout << " --------- Species ---------" << endl;
    for (int i = 0; i < kinetics.species.size(); ++i)
    {
        Kinetics::Specie sp = kinetics.species[i];
        auto specieMap = jvm.Map.create();
        cout << i << " specie: " << sp.name << " col_count: " << sp.col_count << " step_size: " << sp.step_size << endl;
        for (int j = 0; j < sp.params.size(); ++j)
        {
            auto targetList = jvm.List.create();
            Kinetics::Param p = sp.params[j];
            cout << " " << j << " param context: " << p.context << " functional " << p.functional << endl;
            cout << "  - Targets - " << endl << "   ";
            for (int k = 0; k < p.targets.size(); ++k)
            {
                cout << " " << p.targets[k];
            }
            cout << endl;
            cout << "  - Target in subcolor - " << endl << "   ";
            for (int k = 0; k < p.target_in_subcolor.size(); ++k)
            {
                cout << " " << p.target_in_subcolor[k];
                targetList.add(jvm.Byte.valueOf(p.target_in_subcolor[k]).object());
            }
            cout << endl;
            cout << "  - Requirements - " << endl;
            for (auto iter = p.requirements.begin(); iter != p.requirements.end(); ++iter) {
                cout << "   " << iter->first << " -> ";
                Levels sec = iter->second;
                for (int k = 0; k < sec.size(); ++k)
                {
                    cout << " " << sec[k];
                }
                cout << endl;
            }
            specieMap.put(env->NewStringUTF(p.context.c_str()), targetList.object());
        }
        nativeModel.specieContextTargetMapping.put(env->NewStringUTF(sp.name.c_str()), specieMap.object());
    }

	structure = ConstructionManager::computeStructure(model, kinetics);

	//count parameters
	vector<int> subspaceSizes;
	int total = 1;
	for (int i = 0; i < kinetics.species.size(); ++i)
	{
		factory.variableOrdering.add(env->NewStringUTF(kinetics.species[i].name.c_str()));
		total *= kinetics.species[i].col_count;
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
			/*int offset = 0;
			int step_size = 1;
			for (int k = 0; k < subspaceSizes.size() && step_size != transition.step_size; ++k)
			{
				offset += subspaceSizes[k];
				step_size *= subspaceSizes[k];
			}*/
			auto colors = jvm.ColorSet.createFull(total);//= JVM::ColorSetClass::Instance(transition, offset, total, &(jvm.ColorSet));
			for (long num = 0; num < total; num++) {
			    if (!ColoringFunc::isOpen(num, transition)) {
			        colors.unset(num);
			    }
			}
			std::vector<short> targetLevels = structure.getStateLevels(structure.getTargetID(i, j));
			prepare(jvm, targetLevels, temp, levelsArray);
			auto target = factory.getNode(levelsArray);
			node.addSuccessor(target, colors);
			target.addPredecessor(node, colors);
			std::cout << "Transition copied" << std::endl << std::flush;
		}
	}
	delete temp;

}
