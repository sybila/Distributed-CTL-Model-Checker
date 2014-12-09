#include <iostream>
#include "data_model/Model.h"
#include "parser/parse.cc"
#include "scanner/lex.cc"

using namespace std;

int main(int argc, char** argv) {
	Model<double> model;
    string fileName(argv[1]);
    ifstream modelfile2 (fileName);

    if (modelfile2.is_open()) {

        Parser parser(modelfile2);
        parser.parse();

        model = parser.returnStorage();

        model.RunAbstraction(true);

    }
}
