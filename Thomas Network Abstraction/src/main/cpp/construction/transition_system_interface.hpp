/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef TRANSITION_SYSTEM_INTERFACE_HPP
#define TRANSITION_SYSTEM_INTERFACE_HPP

#include "graph_interface.hpp"

/// Structure with constraints on a transition within a TS
struct TransConst {
   ParamNo step_size; ///< How many bits of a parameter space bitset is needed to get from one targe value to another.
   bool req_dir; ///< true for increase, false for decrease
   ActLevel comp_value; ///< value of the specie that's being questioned
   const Levels & targets; ///< Values of the targets for different parameters for this specie.
};

/// Storing a single transition to neighbour state together with its transition function.
struct TSTransitionProperty : public TransitionProperty {
   TransConst trans_const;

   TSTransitionProperty(const StateID target_ID, const ParamNo _step_size, const bool _req_op, const ActLevel _req_comp, const Levels & _targets)
      : TransitionProperty(target_ID), trans_const({_step_size, _req_op, _req_comp, _targets}) {}
};

/// State having specie levels attached.
struct TSStateProperty : public StateProperty<TSTransitionProperty> {
   Levels levels; ///< Species_level[i] = activation level of specie i.

   TSStateProperty(const StateID ID, const Levels& _species_level)
      : StateProperty<TSTransitionProperty>(ID), levels(_species_level) { } ///< Simple filler, assigns values to all the variables.
};

/// Transition system has states labeled with activity levels and transitions labeleld with constraints.
template<typename StateT>
class TSInterface : public virtual GraphInterface<StateT> {
public:

   /**
    * @brief getTransitionConst returns constrains on a specified transition
    * @param ID of the source state
    * @param trans_no ordinar number of the transition
    * @return reference to the constraints data structure
    */
   virtual inline const TransConst & getTransitionConst(const StateID ID, const size_t trans_no) const {
      return GraphInterface<StateT>::states[ID].transitions[trans_no].trans_const;
   }

   /**
    * @brief getStateLevels return reference to activity levels of this state
    * @param ID of the state
    * @return reference to Levels data structure
    */
   virtual inline const Levels & getStateLevels(const StateID ID) const {
      return GraphInterface<StateT>::states[ID].levels;
   }
};


#endif // TRANSITION_SYSTEM_INTERFACE_HPP
