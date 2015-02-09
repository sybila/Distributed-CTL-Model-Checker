/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef PARSYBONE_PARSING_MANAGER_INCLUDED
#define PARSYBONE_PARSING_MANAGER_INCLUDED

#include "../auxiliary/data_types.hpp"
#include "../auxiliary/SQLAdapter.hpp"
#include "data_parser.hpp"
#include "argument_parser.hpp"

namespace ParsingManager {
   /**
    * @brief parseOptions parse user arguments
    */
   UserOptions parseOptions(const int argc, const char* argv[]) {
      UserOptions user_options;

      vector<string> arguments;
      for (const size_t argn : crange(argc))
         arguments.push_back(argv[argn]);

      // Parse arguments
      ArgumentParser parser;
      user_options = parser.parseArguments(arguments);
      user_options.addDefaultFiles();

      if (user_options.use_textfile) {
         output_streamer.createStreamFile(results_str, user_options.datatext_file);
      }

      return user_options;
   }

   /**
    * @brief parseModel parse model from a model file
    */
   Model parseModel(const string & path, const string & name) {
      DataParser data_parser;
      ifstream file(path + name + MODEL_SUFFIX, ios::in);
      return data_parser.parseNetwork(file);
   }

   /**
    * @brief parseProperty parser a property from a property file
    */
   PropertyAutomaton parseProperty(const string & path, const string & name) {
      DataParser data_parser;
      ifstream file(path + name + PROPERTY_SUFFIX, ios::in);
      return data_parser.parseProperty(file);
   }
}

#endif // PARSYBONE_PARSING_MANAGER_INCLUDED
