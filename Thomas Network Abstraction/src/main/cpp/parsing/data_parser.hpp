/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef PARSYBONE_DATA_PARSER_INCLUDED
#define PARSYBONE_DATA_PARSER_INCLUDED

#include "../auxiliary/output_streamer.hpp"
#include "buchi_parser.hpp"
#include "network_parser.hpp"
#include "time_series_parser.hpp"
#include "parameter_parser.hpp"
#include "../model/model_translators.hpp"
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief Starting point of the model parsing.
///
/// ModelParser is an entry point for parsing of a model file. Most of the parsing is done by dependent classes, ModelParser only sets the parsing up for further usage.
/// For the reference on how to create a model see the manual/README.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class DataParser {
	// Created with and for parsing
   rapidxml::xml_document<>  model_xml; ///< Main parsing node
   unique_ptr<char []>  parsed_data; ///< Data obtained from the stream

   size_t getLine(char * point) const {
      return count(parsed_data.get(), point, '\n') + 1;
   }

	/**
	 * Creates RapidXML document from the input stream.
	 */
   void createDocument(ifstream & input_stream) {
		// Copy input data from stream into a string line by line
		string input_line, input_data;
         for (int lineno = 1; getline(input_stream, input_line); ++lineno) {
			input_data += input_line + "\n";
		}

		// Copy data from String to newly created c-string
		parsed_data.reset(new char[input_data.size() + 1]);
#if defined(_MSC_VER)
		strcpy_s(parsed_data.get(), input_data.size() + 1, input_data.c_str());
#else
		strcpy(parsed_data.get(), input_data.c_str());
#endif

		// Setup the parser with the c-string
		try {
			model_xml.parse<0>(parsed_data.get());
      } catch (rapidxml::parse_error e) {
         string pos = to_string(getLine(e.where<char>()));
         output_streamer.output(error_str, "Parsing exception \"" + string(e.what()) + "\" occured on the line " + pos);
         throw runtime_error("Rapidxml::xml_document<>.parse(char *) failed");
		}
	}
	
public:
	/**
	 * Functions that causes the parser to read the input from the stream, parse it and store model information in the model object.
	 */
   Model parseNetwork(ifstream & input_stream) {
      createDocument(input_stream);
      rapidxml::xml_node<> *network_node = model_xml.first_node();
      if (network_node == 0)
         throw runtime_error("Parser did not find any nodes in the network file.");
      if (strcmp(network_node->name(), "NETWORK") != 0)
         throw runtime_error("Parsed found out that input does not start with the tag <NETWORK>but with the <" + string(network_node->name()) + "> instead");

      Model model;
      NetworkParser::parseNetwork(network_node, model);
      NetworkParser::parseConstraints(network_node, model);
      ParameterParser::parse(network_node, model);

      input_stream.clear();
      input_stream.seekg(0, ios::beg);
      return model;
   }

   /**
    * @brief parseProperties create a property automaton
    * @param input_stream
    */
   PropertyAutomaton parseProperty(ifstream & input_stream) {
      PropertyAutomaton automaton;

      createDocument(input_stream);
      rapidxml::xml_node<> *property_node = model_xml.first_node();
      if (property_node == 0)
         throw runtime_error("Parser did not find any nodes in the property file.");

      if (strcmp(property_node->name(), "AUTOMATON") == 0)
         automaton = BuchiParser::parse(property_node);
      else if (strcmp(property_node->name(), "SERIES") == 0)
         automaton = TimeSeriesParser::parse(property_node);
      else
         throw runtime_error("No property found in the property file.");

      return automaton;
   }
};

#endif // PARSYBONE_DATA_PARSER_INCLUDED
