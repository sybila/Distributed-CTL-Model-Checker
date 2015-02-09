/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef PARSYBONE_DATA_TYPES_INCLUDED
#define PARSYBONE_DATA_TYPES_INCLUDED

#include <algorithm>
#include <climits>
#include <cmath>
#include <chrono>
#include <cstring>
#include <fstream>
#include <iostream>
#include <map>
#include <memory>
#include <numeric>
#include <queue>
#include <regex>
#include <set>
#include <string>
#include <stdexcept>

#include <boost/assert.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/range/numeric.hpp>
#include <boost/range/algorithm.hpp>
#include <boost/range/counting_range.hpp>
#include <boost/range/irange.hpp>

#include <gecode/int.hh>
#include <gecode/search.hh>
#include <gecode/minimodel.hh>

using namespace std;
using namespace Gecode;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// @file This file holds specifications of data types (typedefs and 3rd party classes) used by multiple classes throughout the computation.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
using namespace std;
using boost::lexical_cast;
using boost::bad_lexical_cast;
using boost::is_any_of;
using boost::split;
using boost::trim;

namespace rng = boost::range;
namespace alg = boost::algorithm;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// INPUT/OUTPUT
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// What stream to use in output functions.
enum StreamType {error_str, results_str, verbose_str};

/// Symbol that separates values of a single parametrizations on output.
const string separator = ":";

/// Definition of naming abbreviations for edge labels:
namespace Label {
   const string Activating = "Activating";
   const string ActivatingOnly = "ActivatingOnly";
   const string Inhibiting = "Inhibiting";
   const string InhibitingOnly = "InhibitingOnly";
   const string NotActivating = "NotActivating";
   const string NotInhibiting = "NotInhibiting";
   const string Observable = "Observable";
   const string NotObservable = "NotObservable";
   const string Free = "Free";
}

const string MODEL_SUFFIX = ".pmf";
const string PROPERTY_SUFFIX = ".ppf";
const string DATABASE_SUFFIX = ".sqlite";
const string OUTPUT_SUFFIX = ".out";

const string COMPONENTS_TABLE = "Components";
const string REGULATIONS_TABLE = "Regulations";
const string PARAMETRIZATIONS_TABLE = "Parametrizations";

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// MODEL
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// ID of a graph/automaton state.
typedef size_t StateID; 

/// A single transition between two states.
typedef pair<StateID,StateID> StateTransition;

/// ID of a specie.
typedef size_t SpecieID; 

/// Activity level of a specie.
typedef short ActLevel;

/// Vector of activation levels of species used for labelling of states of KS.
typedef vector<ActLevel> Levels;

/// Transitional values in a state of BA.
typedef vector<Levels> Configurations;

/// What properties may be examined.
enum PropType { LTL, TimeSeries };

/// Types of automata possible for using.
enum AutType {BA_finite, BA_weak, BA_standard};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// COMPUTATION
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// Index of the color - may be a big number.
typedef unsigned long long ParamNo;

/// Index of the color - may be a big number as well.
typedef unsigned long long RoundNo;

/// IDs of predecessors of a state.
typedef vector<StateID> Neighbours;

/// this value represents infinite value - used for showing that a variable is unset
const size_t INF = ~0u;
const ActLevel INF_SHORT = ~((short) 0u);

#endif // PARSYBONE_DATA_TYPES_INCLUDED
