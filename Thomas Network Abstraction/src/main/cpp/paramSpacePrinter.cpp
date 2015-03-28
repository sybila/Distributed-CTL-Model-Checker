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

using namespace std;

int main(int argc, char* argv[])
{
    string filename(argv[1]);
    int param_no = atoi(argv[2]);
    /** load model from file **/

    Kinetics kinetics;
    UnparametrizedStructure structure;

    Model model = ParsingManager::parseModelPath(filename);
    kinetics = ConstructionManager::computeKinetics(model, PropertyAutomaton());
    structure = ConstructionManager::computeStructure(model, kinetics);

    /** copy kinetics data into java model **/
	cout << " --------- Species ---------" << endl;
    for (int i = 0; i < kinetics.species.size(); ++i)
    {
        Kinetics::Specie sp = kinetics.species[i];
        //index of function I want to print out
        const size_t value_num = (param_no / sp.step_size) % sp.col_count;
        cout << " specie: " << sp.name << " col_count: " << sp.col_count << " step_size: " << sp.step_size << endl;
        for (int j = 0; j < sp.params.size(); ++j)
        {
            Kinetics::Param p = sp.params[j];
            cout << p.context << " : " << p.target_in_subcolor[value_num] << endl;
        }
    }

}