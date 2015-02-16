/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#pragma once 

#include "../kinetics/parametrizations_builder.hpp"
#include "../kinetics/parameter_builder.hpp"
#include "../model/property_automaton.hpp"
#include "automaton_builder.hpp"
#include "unparametrized_structure_builder.hpp"
#include "product_builder.hpp"

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief STEP 2 - Builds all the structures and stores them within a ConstructionHolder.
///
/// ConstructionManager overviews the whole process of construction of structures from information contained within a model file.
/// All the objects constructed are stored within a provided CostructionHolder and further acessible only via constant getters.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
namespace ConstructionManager {
	// @brief computeModelProps
	Kinetics computeKinetics(const Model & model, const PropertyAutomaton & property) {
		Kinetics result;

		// Compute parameter values.
		result.species = ParameterBuilder::buildParams(model);
		// Disable non-functioncal contexts (optimization)
		// ParameterHelper::find_functional(model, property, result);
		// Compute exact parametrization for the model.
		ParametrizationsBuilder::buildParametrizations(model, result);

		return result;
	}

	UnparametrizedStructure computeStructure(const Model & model, const Kinetics & kinetics) {
		// Create the UKS
		UnparametrizedStructureBuilder unparametrized_structure_builder(model, PropertyAutomaton(), kinetics);
		UnparametrizedStructure unparametrized_structure = unparametrized_structure_builder.buildStructure();
		//	cout << "hi" << endl;
		unparametrized_structure.printMe();
		return unparametrized_structure;
	}

	/**
	 * Function that constructs all the data in a cascade of temporal builders.
	 */
	ProductStructure construct(const Model & model, const PropertyAutomaton & property, const Kinetics & kinetics) {
		// Create the UKS
		UnparametrizedStructureBuilder unparametrized_structure_builder(model, property, kinetics);
		UnparametrizedStructure unparametrized_structure = unparametrized_structure_builder.buildStructure();
		//	cout << "hi" << endl;
		unparametrized_structure.printMe();

		// Create the Buchi automaton
		AutomatonBuilder automaton_builder(model, property);
		AutomatonStructure automaton = automaton_builder.buildAutomaton();

		// Create the product
		ProductBuilder product_builder;
		ProductStructure product = product_builder.buildProduct(move(unparametrized_structure), move(automaton));
		return product;
	}
}