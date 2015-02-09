/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef COLORING_FUNC_HPP
#define COLORING_FUNC_HPP

#include "../construction/product_structure.hpp"

namespace ColoringFunc {
   /**
    * @return true if this transition is open for given parametrization
    */
   bool isOpen(const ParamNo param_no, const TransConst & trans_cost) {
      const size_t value_num = (param_no / trans_cost.step_size) % trans_cost.targets.size();

      if (trans_cost.req_dir)
         return trans_cost.targets[value_num] > trans_cost.comp_value;
      else
         return trans_cost.targets[value_num] < trans_cost.comp_value;
   }

   /**
    * @return vector of reachable targets from ID for this parametrization
    */
   template <class State>
   vector<StateID> broadcastParameters(const ParamNo param_no, const TSInterface<State> & ts, const StateID ID) {
      // To store parameters that passed the transition but were not yet added to the target
      vector<StateID> param_updates;

      // Cycle through all the transition
      for (size_t trans_num = 0; trans_num < ts.getTransitionCount(ID); trans_num++) {
         StateID target_ID = ts.getTargetID(ID, trans_num);

         // From an update strip all the parameters that can not pass through the transition - color intersection on the transition
         if (ColoringFunc::isOpen(param_no, ts.getTransitionConst(ID, trans_num)))
            param_updates.push_back(target_ID);
      }

      return param_updates;
   }
}

#endif // COLORING_FUNC_HPP
