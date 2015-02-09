#ifndef PARSYBONE_AUTOMATON_PARSER_INCLUDED
#define PARSYBONE_AUTOMATON_PARSER_INCLUDED

#include "xml_helper.hpp"
#include "../model/model.hpp"
#include "../model/property_automaton.hpp"
#include "parsing_commons.hpp"
#include "property_parsing.hpp"

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief This object is responsible for parsing and translation of data related to the tested property.
/// TODO add experiment and min/max accepting
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class BuchiParser {
	/**
	 * Starting from the STATE node, the function parses all the EDGE tags and reads the data from them.
	 */
	static void parseEdges(const rapidxml::xml_node<> * const state_node, StateID source_ID, PropertyAutomaton & automaton) {
		// Regulation data
		string traget_str; size_t target_ID;

		// Step into first STATE tag, end when the current node does not have next sibling (all STATE tags were parsed)
		for (auto edge : XMLHelper::NodesRange(state_node, "EDGE", true)) {
			// Get max value and conver to integer.
			XMLHelper::getAttribute(traget_str, edge, "target");
			target_ID = automaton.findID(traget_str);
			if (target_ID >= automaton.getStatesCount())
				throw invalid_argument(string("Incorrect value as a target of the state ").append(to_string(source_ID)));

			PropertyAutomaton::Constraints cons = ParsingCommons::readConstraints(edge);

			// Add a new regulation to the specified target
			automaton.addEdge(source_ID, target_ID, cons);
		}
	}

	/**
	* Starting from the AUTOMATON node, the function parses all the STATE tags and all their EDGE tags and reads the data from them.
	* If not found, final attribute is defaulted to false and name to state's ordinal number.
	*/
	static void firstParse(const rapidxml::xml_node<> * const automaton_node, PropertyAutomaton & automaton)  {
		// State data
		bool final; string name;

		// Step into first STATE tag, end when the current node does not have next sibling (all STATE tags were parsed)
		SpecieID ID = 0;
		for (auto state : XMLHelper::NodesRange(automaton_node, "STATE", true)) {
			// Find out whether the state is final
			if (!XMLHelper::getAttribute(final, state, "final", false))
				final = false;

			// Get a name of the state, or use its ID as a string
			if (!XMLHelper::getAttribute(name, state, "name", false))
				name = to_string(ID);

			// Create a new state
			automaton.addState(name, final);
			ID++;
		}
	}

	/**
	* Starting from the AUTOMATON node, the function parses all the STATE tags and all their EDGE tags and reads the data from them.
	* If not found, final attribute is defaulted to false and name to state's ordinal number.
	*/
	static void secondParse(const rapidxml::xml_node<> * const automaton_node, PropertyAutomaton & automaton)  {
		// Step into first STATE tag, end when the current node does not have next sibling (all STATE tags were parsed)
		SpecieID ID = 0;
		// Get all the edges of the state and store them to the model.
		for (auto state : XMLHelper::NodesRange(automaton_node, "STATE", true))
			parseEdges(state, ID++, automaton);
	}

public:
	/**
	 * Main parsing function. It expects a pointer to inside of a MODEL node.
	 */
	static PropertyAutomaton parse(const rapidxml::xml_node<> * const automaton_node) {
		PropertyAutomaton automaton(LTL);
		ParsingCommons::parsePropertySetup(automaton_node, automaton);

		// Parse states
		firstParse(automaton_node, automaton);
		// Parse edges for the states from the previous parse
		secondParse(automaton_node, automaton);

		return automaton;
	}
};

#endif // PARSYBONE_AUTOMATON_PARSER_INCLUDED
