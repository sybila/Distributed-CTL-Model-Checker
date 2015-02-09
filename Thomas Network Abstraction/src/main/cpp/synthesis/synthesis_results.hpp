/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef SYNTHESIS_RESULTS_HPP
#define SYNTHESIS_RESULTS_HPP

#include "../auxiliary/data_types.hpp"

struct SynthesisResults {
   map<StateID, size_t> found_depth; ///< when a final state was found
   map<size_t, size_t> depths;


   inline bool isAccepting() {
      return isAccepting(1, INF);
   }


   inline bool isAccepting(const size_t min_acc, const size_t max_acc) {
      return (min_acc <= found_depth.size()) && (max_acc >= found_depth.size());
   }

   /**
    * @brief derive  information from stored final states
    */
   void derive() {


      for (const pair<StateID, size_t> & state : found_depth) {
         auto it = depths.find(state.second);
         if (it != depths.end()) {
            it->second++;
         } else {
            depths.insert(make_pair(state.second, 1u));
         }
      }
   }

   /**
    * @brief getLowerBound obtain the lowest cost in between all the values
    */
   size_t getUpperBound() const {
      if (depths.empty())
         return INF;
      else
         return (depths.rbegin())->first;
   }

   /**
    * @brief getUpperBound obtain the lowest cost in between all the values
    */
   size_t getLowerBound() const {
      if (depths.empty())
         return INF;
      else
         return depths.begin()->first;
   }

   /**
    * @return final states that are reached exactly within the given depth
    */
   vector<StateID> getFinalsAtDepth(const size_t depth) const {
      vector<StateID> states;

      for (const pair<StateID, size_t> & state : found_depth)
         if (state.second == depth)
            states.push_back(state.first);

      return states;
   }
};

#endif // SYNTHESIS_RESULTS_HPP
