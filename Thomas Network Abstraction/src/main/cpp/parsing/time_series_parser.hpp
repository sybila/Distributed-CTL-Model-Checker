#ifndef PARSYBONE_TIME_SERIES_PARSER_INCLUDED
#define PARSYBONE_TIME_SERIES_PARSER_INCLUDED

#include "../auxiliary/common_functions.hpp"

#include "../auxiliary/data_types.hpp"
#include "xml_helper.hpp"
#include "../model/property_automaton.hpp"
#include "../model/model.hpp"
#include "property_parsing.hpp"

#include <algorithm>

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief This class parses takes the time series and builds it into a Buchi automaton.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class TimeSeriesParser {
   /**
    * Function reads an EXPR tag and creates according state and transitions.
    * First state has only transition under given condition. Others have loop if (!expression) and transition if (expression).
    */
   static PropertyAutomaton parseExpressions(const rapidxml::xml_node<> * const series_node) {
      PropertyAutomaton property(TimeSeries);
	  ParsingCommons::parsePropertySetup(series_node, property);

      // Read all the measurements. For each add tt self-loop and conditional step to the next state
      StateID ID = 0;
      for (auto expression : XMLHelper::NodesRange(series_node, "EXPR", true)) {
         property.addState(to_string(ID), false);

		 PropertyAutomaton::Constraints cons = ParsingCommons::readConstraints(expression);

         // Initial state lack the self-loop (optimization)

         property.addEdge(ID, ID + 1, cons);
         if (ID != 0) {
			PropertyAutomaton::Constraints empty_cons;
			empty_cons.values = "tt";
			property.addEdge(ID, ID, empty_cons);
         }
         ID++;
      }

      // Add a final state that marks succesful time series walk
      property.addState(to_string(ID), true);

      return property;
   }

public:
   /**
     * Main parsing function. It expects a pointer to inside of a MODEL node.
     */
   static PropertyAutomaton parse(const rapidxml::xml_node<> * const series_node) {
      // Parse Buchi Automaton
      return parseExpressions(series_node);
   }
};

#endif // PARSYBONE_TIME_SERIES_PARSER_INCLUDED
