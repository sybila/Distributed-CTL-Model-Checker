#ifndef PARAMETER_PARSER_HPP
#define PARAMETER_PARSER_HPP

#include "../model/model_translators.hpp"
#include "../parsing/xml_helper.hpp"

class ParameterParser {
private:
   /**
	* @brief parseParameters Reads the constraints.
	*/
	static vector<string> parseParCons(const rapidxml::xml_node<> * const specie_node) {
		vector<string>  result;

		// List through all the CONSTR nodes.
		for (auto parameter : XMLHelper::NodesRange(specie_node, "CONSTR", false)) {
			string expression;
			XMLHelper::getAttribute(expression, parameter, "expr");
			result.push_back(expression);
		}

		return result;
	}

public:
	/**
	 * @brief parse   Reads explicit parameter specification from the model file.
	 */
	static void parse(const rapidxml::xml_node<> * const network_node, Model & model) {

		for (auto specie : XMLHelper::NodesRange(network_node, "SPECIE", true)) {
			// Get ID of the regulated component
			string name;
			XMLHelper::getAttribute(name, specie, "name", true);
			const SpecieID ID = ModelTranslators::findID(model, name);

			// Create all contexts with all the possible values.
			model.species[ID].par_cons = parseParCons(specie);
		}
	}
};

#endif // PARAMETER_PARSER_HPP
