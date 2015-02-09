/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#pragma once

#include "model_translators.hpp"
#include "../parsing/constraint_parser.hpp"

namespace ModelHelper {
   // @return canonic form of the label
   string readLabel(const string & label) {
      string formula;

      // Find the constrain and return its valuation
      if (label.compare(Label::Activating) == 0)
         formula = "+";
      else if (label.compare(Label::ActivatingOnly) == 0)
         formula = "(+ & !-)";
      else if (label.compare(Label::Inhibiting) == 0)
         formula = "-";
      else if (label.compare(Label::InhibitingOnly) == 0)
         formula = "(- & !+)";
      else if (label.compare(Label::NotActivating) == 0)
         formula = "!+";
      else if (label.compare(Label::NotInhibiting) == 0)
         formula = "!-";
      else if (label.compare(Label::Observable) == 0)
         formula = "(+ | -)";
      else if (label.compare(Label::NotObservable) == 0)
         formula = "(!+ & !-)";
      else if (label.compare(Label::Free) == 0)
         formula = "tt";
      else
         formula = label;

	  return formula;
   }

   // @bounds on component values after propagatin the experiment
   pair<Levels, Levels> getBounds(const Model & model, const PropertyAutomaton & property) {
	   ConstraintParser * cons_pars = new ConstraintParser(model.species.size(), ModelTranslators::getMaxLevel(model));

	   // Impose constraints
	   Levels maxes;
	   rng::transform(model.species, back_inserter(maxes), [](const Model::ModelSpecie & specie){ return specie.max_value; });
	   cons_pars->addBoundaries(maxes, true);
	   cons_pars->applyFormula(ModelTranslators::getAllNames(model), property.getExperiment());

	   // Propagate
	   cons_pars->status();

	   return pair<Levels, Levels>{ cons_pars->getBounds(false), cons_pars->getBounds(true) };
   }
};