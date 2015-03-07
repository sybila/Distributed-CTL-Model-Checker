#include <iostream>
#include "data_model/Model.h"
#include "parser/parse.cc"
#include "scanner/lex.cc"
#include "ssg.h"

using namespace std;

int main(int argc, char** argv) {
	Model<double> model;
    string fileName(argv[1]);
    ifstream modelfile2 (fileName);

    if (modelfile2.is_open()) {

        Parser parser(modelfile2);
        parser.parse();

        model = parser.returnStorage();

        model.RunAbstraction();

		StateSpaceGenerator * generator = new StateSpaceGenerator(model, true);

        std::cout << model.getParamRanges()[0].first << " " << model.getParamRanges()[0].second << std::endl;

		std::vector<std::list<std::pair<double, double> > > paramSpace;
		std::list<std::pair<double, double> > param;
		param.push_back(std::pair<double, double>(model.getParamRanges()[0]));
		paramSpace.push_back(param);

        for (long unsigned int i=0; i<model.getThresholdsForVariable(0).size()-1; i++) {
        	State s(1, {i}, paramSpace);
        	vector<State> data = generator->getSucc(s);	
        	std::cout << "Succ for: " << model.getThresholdForVarByIndex(0, s.getCoors()[0]) << " " << s;
        	for (int i = 0; i < data.size(); ++i)
        	{
        		std::cout << data[i];
        	}
        }

        
       
    }
}
