#include <iostream>
#include <set>
#include "data_model/Model.h"
#include "parser/parse.cc"
#include "scanner/lex.cc"
#include "ssg.h"

using namespace std;

int main(int argc, char** argv) {
	Model<double> model;
    string fileName(argv[1]);
    ifstream modelfile2 (fileName);

    std::cout << "Input file name is " << fileName << std::endl;

    if (modelfile2.is_open()) {

        srand(time(0));

        Parser parser(modelfile2);
        parser.parse();

        model = parser.returnStorage();

        std::size_t checkResult = 0;
        if(!model.checkParameterCombination(checkResult)) {
            cout << "ERROR: equation for variable " << model.getVariable(checkResult) << " has too much of parameters\n";
            return(1);
        }
/*
        for(int i = 0; i < model.getDims(); i++) {
            cout << "Equation for variable " << model.getVariable(i) << " has " << model.eqParamsCount(i) << " real params,\n";
			
			cout << "but using " << model.getParamName(model.getParamIndexForVariable(i)) << " with index " << model.getParamIndexForVariable(i) << endl;
        }
*/
        model.RunAbstraction();

		StateSpaceGenerator * generator = new StateSpaceGenerator(model, true);
		int varIndex = 3;
	    std::string varName = model.getVariable(varIndex);

	    std::vector<std::pair<double, double> > subSpace;
        for(int i = 0; i < model.getDims(); i++) {
			if(i == varIndex) {
				std::pair<double, double> space(0,model.getThresholdsForVariable(i).size()-1);
				subSpace.push_back(space);
			} else {
				std::pair<double, double> space(0,1);
				subSpace.push_back(space);
			}
        }

		vector<State> inits = generator->initAP(varName,Operators::LS,2,subSpace);
		int counter = 10000;

		for(State s : inits) cout << s << endl;

        for(int i = 0; i < inits.size(); i++) {
            State s = inits.at(i);
            if(counter == 0) break;
            //State s = states.at(i);
            vector<State> succs = generator->getSucc(s);
            cout << "Succ for: " << s << endl;
            for(State a : succs){
                cout << a << endl;
                //inits.push_back(a);
            }
            std::cout << std::endl;
            inits.push_back(succs.at(rand()%succs.size()));
            counter--;
        }
/*
        std::cout << model.getParamRanges().at(0).first << " " << model.getParamRanges().at(0).second << std::endl;

		std::vector<std::list<std::pair<double, double> > > paramSpace;
		std::list<std::pair<double, double> > param;
		param.push_back(std::pair<double, double>(model.getParamRanges().at(0)));
		paramSpace.push_back(param);

        for (long unsigned int i=0; i<model.getThresholdsForVariable(0).size()-1; i++) {
        	State s(1, {i,0}, paramSpace);
        	vector<State> data = generator->getSucc(s);	
        	std::cout << "Succ for: " << model.getThresholdForVarByIndex(0, s.getCoors().at(0)) << " " << s;
        	for (int j = 0; j < data.size(); ++j)
        	{
        		std::cout << data.at(j);
        	}
        }
*/
        delete generator;
       
    }
    return(0);
}
