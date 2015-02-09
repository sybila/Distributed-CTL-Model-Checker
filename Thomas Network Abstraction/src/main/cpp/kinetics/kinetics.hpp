/*
* Copyright (C) 2012-2014 - Adam Streck
* This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
* ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
* ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
* For affiliations see http://www.mi.fu-berlin.de/en/math/groups/dibimath and http://sybila.fi.muni.cz/ .
*/

#pragma once

#include "../auxiliary/common_functions.hpp"
#include "../auxiliary/output_streamer.hpp"

struct Kinetics {
	struct Param {
		string context; ///< String representation of the context.
		Levels targets; ///< Towards which level this context may regulate.
		map<SpecieID, Levels> requirements; ///< Levels of the source species this param is relevant to, the levels are sorted.
	
		Levels target_in_subcolor; ///< List of values from different subparametrizations for this specie, share indices between params.
		bool functional; ///< True if the param is permitted to occur by the experiment 
	};
	using Params = vector<Param>;

	struct Specie {
		string name; ///< Name of the specie, shared with specie in model.
		Params params; ///< Vector of parameters, sorted lexicographically by the context.
		ParamNo col_count; ///< Number of subcolors for this specie.
		ParamNo step_size; ///< In the context of the whole parametrization space, how may changes occur between a subcolor of this specie changes?
	};

	vector<Specie> species; ///< Species shared with the model, sorted lexicographically. 

	void printMe() {
		cout << " --------- Species ---------" << endl;
		for (int i = 0; i < species.size(); ++i)
		{
			Specie sp = species[i];
			cout << i << " specie: " << sp.name << " col_count: " << sp.col_count << " step_size: " << sp.step_size << endl;
			for (int j = 0; j < sp.params.size(); ++j)
			{
				Param p = sp.params[j];
				cout << " " << j << " param context: " << p.context << " functional " << p.functional << endl;
				cout << "  - Targets - " << endl << "   ";
				for (int k = 0; k < p.targets.size(); ++k)
				{
					cout << " " << p.targets[k];
				}
				cout << endl;
				cout << "  - Target in subcolor - " << endl << "   ";
				for (int k = 0; k < p.target_in_subcolor.size(); ++k)
				{
					cout << " " << p.target_in_subcolor[k];
				}
				cout << endl;
				cout << "  - Requirements - " << endl;				
				for (auto iter = p.requirements.begin(); iter != p.requirements.end(); ++iter) {
					cout << "   " << iter->first << " -> ";
					Levels sec = iter->second;
					for (int k = 0; k < sec.size(); ++k)
					{
						cout << " " << sec[k];
					}
		           	cout << endl;
		        }
			}
		}
	}
};