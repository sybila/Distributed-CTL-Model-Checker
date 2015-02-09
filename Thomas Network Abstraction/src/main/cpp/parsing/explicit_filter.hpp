/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */


#ifndef EXPLICIT_FILTER_HPP
#define EXPLICIT_FILTER_HPP

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief This class takes parametrizations from the database and rememberes those that are allowed by the database.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#include "../auxiliary/data_types.hpp"
#include "../model/model_translators.hpp"
#include "../kinetics/kinetics_translators.hpp"
#include "../auxiliary/SQLAdapter.hpp"

class ExplicitFilter {
	struct SingleFilter {
		SQLAdapter database;
		vector<size_t> columns;
		Levels current;
	};

	vector<SingleFilter> filters;

public:
	ExplicitFilter() {}

	// @brief prepare add parametrizations that are allowed by given database
	void prepare(const Kinetics & kinetics, const string & filter_file) {
		SingleFilter new_filter;

		// Test for emptyness
		new_filter.database = {};
		new_filter.database.setDatabase(filter_file);
		new_filter.database.accessTable(PARAMETRIZATIONS_TABLE);

		new_filter.columns = new_filter.database.getColumnsByExpression(PARAMETRIZATIONS_TABLE, regex("K_.*")).second;

		// Obtain the parametrizations from the database
		new_filter.current = new_filter.database.getRow<ActLevel>(new_filter.columns);

		filters.emplace_back(move(new_filter));
	}

	/**
	 * @return true iff the parametrization is not filtered out.
	 */
	inline bool isAllowed(const Kinetics & kinetics, const ParamNo param_no) {
		const Levels parametrization = KineticsTranslators::createParamVector(kinetics, param_no);

		for (auto & filter : filters) {
			while (filter.current < parametrization && !filter.current.empty()) 
				filter.current = filter.database.getRow<ActLevel>(filter.columns);
			
			if (filter.current.empty() || filter.current != parametrization)
				return false;
		}
		return true;
	}
};

#endif // EXPLICIT_FILTER_HPP
