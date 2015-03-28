/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef PARSYBONE_ARGUMENT_PARSER_INCLUDED
#define PARSYBONE_ARGUMENT_PARSER_INCLUDED

#include "../auxiliary/output_streamer.hpp"
#include "../auxiliary/user_options.hpp"
#include "../auxiliary/usage.hpp"

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief A class responsible for reading the arguments on the input.
///
/// A sets user options according to the string provided as arguments at the start of the program.
/// All values that are not used for direct setup are stored within a UserOptions class.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ArgumentParser {
   enum Filetype {database, datatext};

   /**
    * Obtain parameters for synthesis distribution.
    */
   int getDistribution(UserOptions & user_options, vector<string>::const_iterator position, const vector<string>::const_iterator & end) {
      // Get numbers
      try {
         if (++position == end)
            throw invalid_argument("The number of processes and the total count of processes are missing");
         user_options.process_number = lexical_cast<size_t>(*position);
         if (++position == end)
            throw invalid_argument("The number of processes or the total count of processes is missing");
         user_options.processes_count = lexical_cast<size_t>(*position);
      } catch (bad_lexical_cast & e) {
         throw invalid_argument("Error while parsing the modifier --dist" + string(e.what()));
      }

      // Assert that process ID is in the range
      if (user_options.process_number > user_options.processes_count)
         throw runtime_error("Error while parsing the modifier --dist - ID of the process is bigger than number of processes");

      return 2;
   }

   /**
    * Obtain parameters for bounded computation.
    */
   int getBound(UserOptions & user_options, vector<string>::const_iterator position, const vector<string>::const_iterator & end) {
      try {
         if (++position == end)
            throw invalid_argument("Bound values is missing");
         user_options.bound_size = lexical_cast<size_t>(*position);
      } catch (bad_lexical_cast & e) {
         throw invalid_argument("Error while parsing the modifier --bound" + string(e.what()));
      }

      return 1;
   }

   /**
    * @brief getFileName   stores path to a file based on its type in user options
    * @param filetype
    * @param position
    * @param end
    * @return how many arguments you have used
    */
   int getFileName(UserOptions & user_options, const Filetype & filetype, vector<string>::const_iterator position, const vector<string>::const_iterator & end) {
      if (++position == end) {
         throw invalid_argument("Filename missing after the modifier " + *(--position));
         return 0;
      }

      switch (filetype) {
      case database:
         user_options.database_file = *position;
         user_options.use_database = true;
         break;
      case datatext:
         user_options.datatext_file = *position;
         user_options.use_textfile = true;
         break;
      }

      return 1;
   }

   /**
    * Get the position of the specified argument.
    */
   const vector<string>::const_iterator getArgumentPosition(const string & argument, const vector<string> & arguments) {
      // Get the position of the current modifier.
      auto position = arguments.begin();
      for(auto arg:arguments) {
         if (argument.compare(arg) != 0)
            position++;
         else
            return position;
      }
      throw runtime_error("Argument not found in arguments (internal error).");
   }

   /**
    * Get the full-text modifiers.
    */
   int parseModifier(UserOptions & user_options, const string & modifier, const vector<string> & arguments) {
      auto position = getArgumentPosition(modifier, arguments);

      // Apply the modifier.
      if (position->compare("--help") == 0) {
         cout << getUsage();
         exit(0);
      } else if (position->compare("--ver") == 0) {
         cout << "Parsybone version: " << getVersion() << endl;
         exit(0);
      } else if (position->compare("--dist") == 0) {
         return getDistribution(user_options, position, arguments.end());
      } else if (position->compare("--text") == 0) {
         user_options.use_textfile = true;
         return getFileName(user_options, datatext, position, arguments.end());
      } else if (position->compare("--data") == 0) {
         user_options.use_database = true;
         return getFileName(user_options, database, position, arguments.end());
      } else if (position->compare("--bound") == 0) {
         return getBound(user_options, position, arguments.end());
      } else {
         throw invalid_argument("Unknown modifier " + *position);
      }
   }

   /**
    * Function that parses switches in the string that starts with a "-" symbol.
    *
    * @param argument	iterator pointer to the string to read
    */
   void parseSwitch(UserOptions & user_options, const char s){
      switch (s) {
      case 'W':
         user_options.use_long_witnesses = true;

      case 'w':
         user_options.compute_wintess = true;
         break;

      case 'r':
         user_options.compute_robustness = true;
         break;

      case 'v':
         user_options.be_verbose = true;
         break;

      case 'f':
         user_options.use_textfile = true;
         break;

      case 'd':
         user_options.use_database = true;
         break;

      case 'c':
         user_options.output_console = true;
         break;

      case 'm':
         user_options.minimalize_cost = true;
         break;

      case 'n':
         user_options.produce_negative = true;
         break;

      default:
         throw invalid_argument("Unknown switch -" + std::string(1,s));
         break;
      }
   }

   /**
     * @brief referenceModel save the model name
     * @param model set to true iff the source file is the model file
     */
   void referenceSource(UserOptions & user_options, const string & source, bool model) {
      // References are given by what sort of source we have - model / property
      string & name = model ? user_options.model_name : user_options.property_name;
      string & path = model ? user_options.model_path : user_options.property_path;
      const string & SUFFIX = model ? MODEL_SUFFIX : PROPERTY_SUFFIX;

      // If the model is alredy parsed.
      if (!name.empty())
         throw invalid_argument("Model file (file with a " + MODEL_SUFFIX + " suffix) occurs multiple times on the input, only a single occurence is allowed");

      // Attach the stream to the file.
      ifstream file(source, ios::in);
      if (file.fail()) {
         remove(path.c_str());
         throw runtime_error("Program failed to open an intput stream file: " + source);
      }

      // Store the models name.
      auto pos1 = source.find_last_of("/\\") + 1; // Remove the prefix (path), if there is any.
      auto pos2 = source.find(SUFFIX) - pos1; // Remove the suffix.
      path = source.substr(0, pos1);
      name = source.substr(pos1, pos2);
   }

public:
   /**
    * Take all the arguments on the input and store information from them.
    *
    * @param argc value passed from main function
    * @param argv	value passed from main function
    */
   UserOptions parseArguments (const vector<string> & arguments) {
      UserOptions user_options;
      int skip = 1;

      // Cycle through arguments (skip the first - name of the program).
      for (const string & argument:arguments) {
         if (skip-- > 0)
            continue;
         // There can be multiple switches after "-" so go through them in the loop.
         else if (argument[0] == '-' && argument[1] != '-') {
            for (char s:argument.substr(1)) {
               parseSwitch(user_options, s);
            }
         }
         else if (argument[0] == '-' && argument[1] == '-') {
            skip = parseModifier(user_options, argument, arguments);
         }
         // If it is a model file.
         else if (argument.find(MODEL_SUFFIX) != argument.npos) {
            referenceSource(user_options, argument, true);
         }
         // If it is a property file.
         else if (argument.find(PROPERTY_SUFFIX) != argument.npos) {
            referenceSource(user_options, argument, false);
         }
         else if (argument.find(DATABASE_SUFFIX) != argument.npos) {
            ifstream file(argument);
            if (!file) {
               remove(argument.c_str());
               throw invalid_argument("Filtering database " + argument + " does not exist.");
            }
            user_options.filter_databases.push_back(argument);
         }
         else {
            throw runtime_error("Wrong argument on the input stream: " +  argument);
         }
      }

      // Throw an exception if no model or property file was found.
      if (user_options.model_name.empty())
         throw (invalid_argument("Model file (file with a " + MODEL_SUFFIX + " suffix) is missing"));
      if (user_options.property_name.empty())
         throw (invalid_argument("Property file (file with a " + PROPERTY_SUFFIX + " suffix) is missing"));

      return user_options;
   }
};

#endif // PARSYBONE_ARGUMENT_PARSER_INCLUDED
