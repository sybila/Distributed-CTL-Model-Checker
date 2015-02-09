#ifndef PARSING_COMMONS_HPP
#define PARSING_COMMONS_HPP

#include "../auxiliary/data_types.hpp"

namespace ParsingCommons {
	PropertyAutomaton::Constraints readConstraints(const rapidxml::xml_node<> * const node) {
		PropertyAutomaton::Constraints constraints;

		// Labelled transition to the next measurement
		XMLHelper::getAttribute(constraints.values, node, "values");
		constraints.values.erase(
			remove_if(constraints.values.begin(), constraints.values.end(), static_cast<int(*)(int)>(isspace)),
			constraints.values.end());

		// Resolve constraints
		XMLHelper::getAttribute(constraints.transient, node, "transient", false);
		XMLHelper::getAttribute(constraints.stable, node, "stable", false);
		if (constraints.transient && constraints.stable)
			throw runtime_error("An edge cannot be both transient and stable.");

		return constraints;
	}

	/* Read common content of properties */
	void parsePropertySetup(const rapidxml::xml_node<> * const property_node, PropertyAutomaton & property) {
		size_t val; string expr;
		property.min_acc = XMLHelper::getAttribute(val, property_node, "min_acc", false) ? val : 1;
		property.max_acc = XMLHelper::getAttribute(val, property_node, "max_acc", false) ? val : INF;
		property.experiment = XMLHelper::getAttribute(expr, property_node, "experiment", false) ? expr : "tt";
	}

	/* True if the name follows the necessities of being a specie name, that is:
	 * - starts with a letter or an underscore
	 * - contains only letters, digits or underscore
	 */
	bool isValidSpecName(const string & spec_name) {
		if (spec_name.empty())
			return false;
		bool valid = isalpha(spec_name[0]) || spec_name[0] == '_';
		for (char ch : spec_name)
			valid &= isalnum(ch) || ch == '_';
		return valid;
	}

	/* Throws an exception if a name is not valid. */
	void specNameExc(const string & spec_name) {
		if (spec_name.empty())
			throw invalid_argument("An empty specie name occured.");
		throw invalid_argument("Name of the specie \"" + spec_name + "\" is incorrect. Specie names can contain only letters,numbers and underscore.");
	}
}

#endif // PARSING_COMMONS_HPP
