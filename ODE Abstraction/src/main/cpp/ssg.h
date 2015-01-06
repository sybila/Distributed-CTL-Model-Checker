#include <stdio.h>
#include <iostream>
#include <string>
#include <array>
#include <list>
#include <utility>
#include <algorithm>
#include <cmath>
#include <limits>

#include "./data_model/Model.h"

using namespace std;

typedef unsigned int uint;

class ParameterSpace {
	
	size_t psSize;
	//list<pair<double,double> > psp[psSize];
	//array<list<pair<double,double> >, psSize> ps;
	vector<list<pair<double,double> > > ps;
	
public:

	//-------------------------------Interface---------------------------------------------

	//constructor
	ParameterSpace() {}
	ParameterSpace(vector<list<pair<double, double> > > data);
	ParameterSpace(const size_t psSize, const vector<pair<double,double> >& paramRanges);
	ParameterSpace(const ParameterSpace& copy);

	void setParamRange(size_t paramIndex, pair<double,double> paramRange);
	void addParamRange(size_t paramIndex, pair<double,double> paramRange);
	
	static ParameterSpace derivedParamSpace(const ParameterSpace& ps, int pIndex, double pValue);
	static ParameterSpace derivedParamSpace(const ParameterSpace& ps, int pIndex, double rpValue, double lpValue);
	
	const list<pair<double,double> >& getParamList(size_t index) const;
//	array<list<pair<double,double> >, psSize>& getParamSpace();
	const vector<list<pair<double,double> > >& getParamSpace() const;
	const size_t getParamSpaceSize() const { return psSize; }
	
	ParameterSpace& operator=(const ParameterSpace& copy);
	friend ostream& operator<<(ostream& out,const ParameterSpace& ps);
	
};

//-------------------------------Implementation-------------------------------------------
	
ParameterSpace::ParameterSpace(const size_t psSize, const vector<pair<double,double> >& paramRanges) : psSize(psSize) {
//	ps = array<list<pair<double,double> >,psSize>();
	ps = vector<list<pair<double,double> > >(psSize);
	for(int i = 0; i < paramRanges.size(); i++) {
		ps.at(i) = list<pair<double,double> >();
		ps.at(i).push_back(paramRanges.at(i));
	}
}

ParameterSpace::ParameterSpace(const ParameterSpace& copy) {
	*this = copy;
}	

ParameterSpace::ParameterSpace(vector<list<pair<double, double> > > data) {
  this->ps = data;
}


void ParameterSpace::setParamRange(size_t paramIndex, pair<double,double> paramRange) {
	if(ps.empty() || ps.size() < paramIndex || paramIndex < 0) {
		cerr << "ERROR: Wrong parameter index (" << paramIndex << "), size of ps is " << ps.size() << endl;
		return;
	}
	
	if(paramRange.first > paramRange.second) {
		cerr << "ERROR: New interval of parameters is descending (Must be ascending).\n";
		return;
	}	
	
	ps.at(paramIndex) = list<pair<double,double> >();
	ps.at(paramIndex).push_back(paramRange);
}

void ParameterSpace::addParamRange(size_t paramIndex, pair<double,double> paramRange) {
	if(ps.empty() || ps.size() < paramIndex || paramIndex < 0) {
		cerr << "ERROR: Wrong parameter index (" << paramIndex << "), size of ps is " << ps.size() << endl;
		return;
	}
	
	if(paramRange.first > paramRange.second) {
		cerr << "ERROR: Added interval of parameters is descending (Must be ascending).\n";
		return;
	}

	list<pair<double,double> >::iterator  liter = ps.at(paramIndex).begin();
	for(; liter != ps.at(paramIndex).end(); ) {
		//TODO: treba spravne umistnit sub interval medzi ostatne
		//spec. pripady: spojenie 2 alebo 3 intervalov do jednoho (jeden z nich je ten novy)
		//				 ukladanie na koniec alebo na zaciatok
		//				 zistit co sa stane ak chcem pridat interval, ktoreho cast (alebo cely) je uz pritomna

		if(paramRange.second < liter->first) {
			//added interval doesn't cros this intervals
			ps.at(paramIndex).insert(liter,paramRange);
			break;
		}
		if(paramRange.second >= liter->first && paramRange.second <= liter->second && paramRange.first < liter->first) {
			//added interval's right boundary crosses this interval
			liter->first = paramRange.first;
			break;
		}
		if(paramRange.first >= liter->first && paramRange.second <= liter->second) {
			//entire added interval is subset of this interval
			break;
		}
		if(paramRange.first > liter->first && paramRange.first <= liter->second) {
			//added interval's left boundary crosses this interval
			paramRange.first = liter->first;
		}
		if(paramRange.first <= liter->first && paramRange.second > liter->second) {
			//entire this interval is subset of added interval so this interval is deleted and would be replaced by added interval
			liter = ps.at(paramIndex).erase(liter);
		} else {
			liter++;
		}
		if(liter == ps.at(paramIndex).end()) {
			//added interval is greater than previous intervals
			ps.at(paramIndex).insert(liter,paramRange);
			liter = ps.at(paramIndex).end();
		}
	}
}

ParameterSpace ParameterSpace::derivedParamSpace(const ParameterSpace& ps, int pIndex, double lpValue, double rpValue) {
	ParameterSpace newPS(ps);
	
	if(lpValue <= ps.ps.at(pIndex).front().first && rpValue >= ps.ps.at(pIndex).back().second) {
		//whole param interval (with index pIndex) of this param space will be in returned param space
		return newPS;
		
	} else {
		//param space need to be cut from left side
		for(auto liter = ps.ps.at(pIndex).begin(); liter != ps.ps.at(pIndex).end(); liter++) {
				if(liter->first >= lpValue) {
					//whole param sub-interval (with index pIndex) of this param space will be in returned param space
					break;
				}
				if(liter->second >= lpValue) {
					//this specific interval need to be cut from [first,second] to [lpValue,second] 
					
					newPS.ps.at(pIndex).pop_front();
					newPS.ps.at(pIndex).push_front(pair<double,double>(lpValue,liter->second));
					break;
				}
				newPS.ps.at(pIndex).pop_front();
		}
		
		//param space need to be cut from right side
		for(auto liter = ps.ps.at(pIndex).rbegin(); liter != ps.ps.at(pIndex).rend(); liter++) {
				if(liter->second <= rpValue) {
					//whole param sub-interval (with index pIndex) of this param space will be in returned param space
					break;
				}
				if(liter->first <= rpValue) {
					//this specific interval need to be cut from [first,second] to [first,rpValue] 
					
					newPS.ps.at(pIndex).pop_back();
					newPS.ps.at(pIndex).push_back(pair<double,double>(liter->first,rpValue));
					break;
				}
				newPS.ps.at(pIndex).pop_back();
		}
	}
	
	return newPS;
}

ParameterSpace ParameterSpace::derivedParamSpace(const ParameterSpace& ps, int pIndex, double pValue) {
	ParameterSpace newPS(ps);
	
	if(pValue > 0) {
	
		if(abs(pValue) <= ps.ps.at(pIndex).back().second) {
			for(auto liter = ps.ps.at(pIndex).begin(); liter != ps.ps.at(pIndex).end(); liter++) {
				if(liter->first >= abs(pValue)) {
					//whole param interval (with index pIndex) of this param space will be in returned param space
					
//					newPS.ps.at(pIndex) = list(liter, this->ps.at(pIndex).end());
					return newPS;
				}
				if(liter->second >= abs(pValue)) {
					//this specific interval need to be cut from [first,second] to [abs(pValue),second] 
					//and rest of param interval (with index pIndex) of this param space will be in returned param space
					
//					newPS.ps.at(pIndex) = list(++liter, this->ps.at(pIndex).end());
					newPS.ps.at(pIndex).pop_front();
					newPS.ps.at(pIndex).push_front(pair<double,double>(abs(pValue),liter->second));
					return newPS;
				}
				newPS.ps.at(pIndex).pop_front();
			}
		} else {
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			//!!!I don't know if this param interval will be empty or what!!!!
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			cerr << "Error: 'pValue' is greter than 'ps.at(pIndex).back().second'\n";
		}		
	} else if(pValue < 0) {
	
		if(abs(pValue) >= ps.ps.at(pIndex).front().first) {
			for(auto liter = ps.ps.at(pIndex).rbegin(); liter != ps.ps.at(pIndex).rend(); liter++) {
				if(liter->second <= abs(pValue)) {
					//whole param interval (with index pIndex) of this param space will be in returned param space
					
					return newPS;
				}
				if(liter->first <= abs(pValue)) {
					//this specific interval need to be cut from [first,second] to [first,abs(pValue)]
					//and rest of param interval (with index pIndex) of this param space will be in returned param space
					
					newPS.ps.at(pIndex).pop_back();
					newPS.ps.at(pIndex).push_back(pair<double,double>(liter->first,abs(pValue)));
					return newPS;
				}
				newPS.ps.at(pIndex).pop_back();
			}
		} else {
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			//!!!I don't know if this param interval will be empty or what!!!!
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			cerr << "Error: 'pValue' is lesser than 'ps.at(pIndex).front().first'\n";			
		}
	}
	
	cerr << "Error: Parameter space wasn't cut\n";
	return newPS;
}

const list<pair<double,double> >& ParameterSpace::getParamList(size_t index) const {
	return ps.at(index);
}

const vector<list<pair<double,double> > >& ParameterSpace::getParamSpace() const {
//array<list<pair<double,double> >, psSize>& ParameterSpace::getParamSpace() {
	return ps;
}

ParameterSpace& ParameterSpace::operator=(const ParameterSpace& copy) {
	if(this != &copy) {
		psSize = copy.psSize;
		ps = copy.ps;
	}
	return *this;
}


ostream& operator<<(ostream& out,const ParameterSpace& ps) {

	if(!ps.ps.empty()) {
	
		out << "[";
		for(auto l = ps.ps.at(0).begin(); l != ps.ps.at(0).end(); l++) {
				out << "(" << l->first << "," << l->second << ")";
			}
		for(int v = 1; v < ps.ps.size(); v++) {
			out << " x ";
			for(auto li = ps.ps.at(v).begin(); li != ps.ps.at(v).end(); li++) {
				out << "(" << li->first << "," << li->second << ")";
			}
		}
		out << "]";
	}
	return out;
}


//==================NEW CLASS STATE===========================================

class State {
	
	size_t dims;
//	array<size_t, dims> coors;
	vector<size_t> coors;
	ParameterSpace colors;
	
public:

	//-------------------------Interface--------------------------------------
	
//	State(const size_t dims, array<size_t, dims> coors, ParameterSpace colors);
	State(const size_t dims, vector<size_t> coors, ParameterSpace colors);
	State(const State& copy) { *this = copy; }
	
	void setColors(ParameterSpace c) { colors = c; }
		
	const size_t getDims() const { return dims; }
//	const array<size_t,dims>& getCoors() const;
	const vector<size_t>& getCoors() const { return coors; }	
	const ParameterSpace getColors() const { return colors; }
	ParameterSpace& getColors() { return colors; }
	
	State& operator=(const State& copy);
	bool operator==(const State& s);
	bool operator!=(const State& s) { return !(*this == s); }
	
	friend ostream& operator<<(ostream& out,const State& st);
	
protected:

	void setDims(const size_t d) { dims = d; }
	void setCoors(const vector<size_t>& c) { coors = c; }
		
};

//-------------------------Implementation---------------------------------

//State::State(const size_t dims, array<size_t, dims> coors, ParameterSpace colors) : dims(dims), coors(coors), colors(colors) {
State::State(const size_t dims, vector<size_t> coors, ParameterSpace colors) : dims(dims), coors(coors), colors(colors) {
}


State& State::operator=(const State& copy) {
	if(this != &copy) {
		dims = copy.dims;
		coors = copy.coors;
		colors = copy.colors;
	}
	return *this;
}	


bool State::operator==(const State& s) {
	if(dims != s.dims)
		return false;
	
	for(int i = 0; i < dims; i++) {
		if(coors.at(i) != s.coors.at(i))
			return false;
	}
	
	return true;
}


ostream& operator<<(ostream& out,const State& st) {
	out << "[";
	if(st.getCoors().size() > 0)
		out << st.getCoors().at(0);
	for(size_t i = 1; i < st.getCoors().size(); i++) {
		out << "," << st.getCoors().at(i);
	}
	out << "] : ";
	out << st.colors << endl;
	return out;
}



//===========================NEW CLASS OPERATORS=========================

enum class Operators { GR, LS, GREQ, LSEQ };

ostream& operator<<(ostream& out,const Operators& op) {
	switch(op) {
	case Operators::GR:
		out << ">";
		break;
	case Operators::LS:
		out << "<";
		break;
	case Operators::GREQ:
		out << ">=";
		break;
	case Operators::LSEQ:
		out << "<=";
		break;
	default:
		cerr << "Error: Unknown value of input parameter (Operators) in function \"operator<<\"\n";
	}
	return out;
}


//===========================NEW CLASS STATE_SPACE_GENERATOR=========================

class StateSpaceGenerator {
	
	// if it would be parametrized state space for colored model checking
	bool parametrized;
	
	// piece-wise multi-affine data model waiting for rectangular abstraction
	Model<double> dataModel;
	

	//--------------------------Interface-----------------------------------
	
public:

	StateSpaceGenerator(Model<double>& dm, bool parametrized = false);

	//TODO: mozno do buducna zmenit thres aby mohlo byt lubovolny double a pre neho budem hladat stavy mensie 
	//		(pripadne rovne ak priamo do nejakeho spada tato hodnota), pre vacsie (alebo rovne) ekvivalentne
	vector<State> initAP(string var, Operators op, double thres, const vector<pair<double,double> >& subspace);
	vector<State> init();

	vector<State> getPred(const State& s, bool biggestConvexHullOfParamSubspace = true);
	vector<State> getSucc(const State& s, bool biggestConvexHullOfParamSubspace = true);

private:

	vector<State> getPredOrSucc(const State&  s, bool isSucc, bool biggestConvexHullOfParamSubspace = true);
	double value(const vector<size_t>& vertex, size_t dim, int& paramIndex, double& denom);
	vector<vector<size_t> > getRightVertices(const State& s, size_t dim, bool lower);
	void getRightVerticesRecursive(const State& s, size_t dim, bool lower, vector<vector<size_t> >& vertices, vector<size_t>& coors, size_t actIndex);

//	void getRightStatesRecursive(size_t begin, size_t end, size_t varIndex, vector<State>& states, vector<size_t>& coors, size_t actIndex);
	void getRightStatesRecursive(vector<pair<size_t,size_t> >& thresholdsIndexis, vector<State>& states, vector<size_t>& coors, size_t actIndex);
	vector<State> getRightInitStates(int varIndex, size_t begin = 0, size_t end = 0, const vector<pair<double,double> >& subspace = vector<pair<double,double> >());	
	
};

//---------------------------Implementation-------------------------------

StateSpaceGenerator::StateSpaceGenerator(Model<double>& dm, bool p) : dataModel(dm), parametrized(p) {
}


vector<State> StateSpaceGenerator::getPred(const State& s, bool biggestConvexHullOfParamSubspace) {
	return getPredOrSucc(s,false,biggestConvexHullOfParamSubspace);
}

vector<State> StateSpaceGenerator::getSucc(const State& s, bool biggestConvexHullOfParamSubspace) {
	return getPredOrSucc(s,true,biggestConvexHullOfParamSubspace);
}

vector<State> StateSpaceGenerator::getPredOrSucc(const State&  s, bool isSucc, bool biggestConvexHullOfParamSubspace) {

	vector<State> returnedStates;
	
	bool hasSelfloop = true;

	// cycle through all variables (species / molecules)
	for(int v = 0; v < dataModel.getDims(); v++) {
	
		bool lowerPositiveDirection = false;
		bool lowerNegativeDirection = false;		
		bool upperPositiveDirection = false;
		bool upperNegativeDirection = false;		
	
//		if(s.getCoors().at(v) != 0) 
		{
			//I want to check lower state in this dimension only if state 's' is not at the bottom in this dimension

			vector<vector<size_t> > vertices = getRightVertices(s, v, true);

			vector<double> paramValues;
			int paramIndex = -1;
//			double oneParamValue = numeric_limits<double>::max();
			double mostRightOneValue = numeric_limits<double>::max();
			double mostLeftOneValue = numeric_limits<double>::lowest();
			double derivationValue = 0.0;

			// cycle for every vertices in lower (n-1)-dimensional facet of this state
			for(int i = 0; i < vertices.size(); i++) {

//				paramIndex = -1;
				double denom = 0.0;
			
				derivationValue = value(vertices.at(i),v,paramIndex,denom);
			
				if(paramIndex != -1) {

					if(abs(denom) != 0) {
					
						paramValues.push_back(derivationValue/(-denom) == -0 ? 0 : derivationValue/(-denom));
						//cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;
						
						if(s.getColors().getParamSpace().at(paramIndex).empty())
							cerr << "Error: no interval for parameter " << dataModel.getParamName(paramIndex) << endl;
						
						// lowest and highest values of parameter space for chosen variable
						double lowestParamValue = s.getColors().getParamSpace().at(paramIndex).front().first;
						double highestParamValue = s.getColors().getParamSpace().at(paramIndex).back().second;
						
						if(lowestParamValue > highestParamValue)
							swap(lowestParamValue, highestParamValue);
					
						// works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
						if(denom < 0) {
							if(!isSucc && lowestParamValue <= paramValues.back()) {
								lowerPositiveDirection = true;
								
								if( mostRightOneValue == numeric_limits<double>::max() ||
								(biggestConvexHullOfParamSubspace && mostRightOneValue < paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostRightOneValue > paramValues.back()) )
									mostRightOneValue = paramValues.back();
							}
							if(isSucc && highestParamValue >= paramValues.back()) {
								lowerNegativeDirection = true;
								
								if( mostLeftOneValue == numeric_limits<double>::lowest() || 
								(biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
									mostLeftOneValue = paramValues.back();
							}
						} else { // denom > 0
							if(isSucc && lowestParamValue <= paramValues.back()) {
								lowerNegativeDirection = true;
								
								if( mostRightOneValue == numeric_limits<double>::max() ||
								(biggestConvexHullOfParamSubspace && mostRightOneValue < paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostRightOneValue > paramValues.back()) )
									mostRightOneValue = paramValues.back();
							}
							if(!isSucc && highestParamValue >= paramValues.back()) {
								lowerPositiveDirection = true;
								
								if( mostLeftOneValue == numeric_limits<double>::lowest() || 
								(biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
									mostLeftOneValue = paramValues.back();
							}
						}
					
/*					
						if(isSucc) {	
												
							if(paramValues.back() < 0) {
								lowerNegativeDirection = true;
							
								if(oneParamValue > paramValues.back())
									oneParamValue = paramValues.back();
							} else {
								lowerPositiveDirection = true;

							}
						} else {
						
							if(paramValues.back() < 0) {
								lowerNegativeDirection = true;			

							} else {
								lowerPositiveDirection = true;
								
								if(oneParamValue > paramValues.back())
									oneParamValue = paramValues.back();
							}
						} */
						
					} else {	// abs(denom) == 0 (ERGO: it might be at border of state space)
						//cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
						if(derivationValue < 0) {
							lowerNegativeDirection = true;
							
							if(isSucc) {
								mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
								mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
							}
						} else {
							lowerPositiveDirection = true;
							
							if(!isSucc) {
								mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
								mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
							}
						} 						
					}
				} else {	// paramIndex == -1 (ERGO: no unknown parameter in equation)
					//cerr << "derivation = " << derivationValue << endl;
					if(derivationValue < 0) {
						lowerNegativeDirection = true;
					} else {
						lowerPositiveDirection = true;
					} 
				}
			}
			
			//cerr << "most left  param value on lower facet: " << mostLeftOneValue << endl;
			//cerr << "most right param value on lower facet: " << mostRightOneValue << endl;
		
			if(s.getCoors().at(v) != 0)	{
				if(!isSucc) {
					//If I want predecessors of state 's'
				
					if(lowerPositiveDirection) {
						//There exists edge from lower state to state 's'

						vector<size_t> newStateCoors(s.getCoors());
						newStateCoors.at(v) = newStateCoors.at(v) - 1;

						ParameterSpace newPS;
						if(paramIndex != -1) {
							//Parameter space needs to be cut for this edge
	//						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
							newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,mostLeftOneValue,mostRightOneValue);						
						} else {
							//Edge is for whole parameter space						
							newPS = ParameterSpace(s.getColors());
						}
				
						State newState(s.getDims(),newStateCoors,newPS);
					
						returnedStates.push_back(newState);
					}
				} else {
					//If I want successors of state 's'
					if(lowerNegativeDirection) {
						//There exists edge from lower state to state 's'

						vector<size_t> newStateCoors(s.getCoors());
						newStateCoors.at(v) = newStateCoors.at(v) - 1;

						ParameterSpace newPS;
						if(paramIndex != -1) {
							//Parameter space needs to be cut for this edge
	//						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
							newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,mostLeftOneValue,mostRightOneValue);
						} else {
							//Edge is for whole parameter space						
							newPS = ParameterSpace(s.getColors());
						}
				
						State newState(s.getDims(),newStateCoors,newPS);
					
						returnedStates.push_back(newState);
					}
				}
			}
		}		
		
//		if(s.getCoors().at(v) != dataModel.getThresholdsForVariable(v).size() -2) 
		{
			//I want to check upper state in this dimension only if state 's' is not at the top in this dimension
		
			vector<vector<size_t> > vertices = getRightVertices(s, v, false);			
			
			vector<double> paramValues;
			int paramIndex = -1;
//			double oneParamValue = numeric_limits<double>::max();
			double mostRightOneValue = numeric_limits<double>::max();
			double mostLeftOneValue = numeric_limits<double>::lowest();
			double derivationValue = 0.0;

			// cycle for every vertices in higher (n-1)-dimensional facet of this state
			for(int i = 0; i < vertices.size(); i++) {

//				paramIndex = -1;
				double denom = 0.0;
			
				derivationValue = value(vertices.at(i),v,paramIndex,denom);
			
				if(paramIndex != -1) {
				
					if(abs(denom) != 0) {
						paramValues.push_back(derivationValue/(denom != 0.0 ? -denom : 1) == -0 ? 0 : derivationValue/(denom != 0.0 ? -denom : 1));
						//cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;
						
						if(s.getColors().getParamSpace().at(paramIndex).empty())
							cerr << "Error: no interval for parameter " << dataModel.getParamName(paramIndex) << endl;

						// lowest and highest values of parameter space for chosen variable						
						double lowestParamValue = s.getColors().getParamSpace().at(paramIndex).front().first;
						double highestParamValue = s.getColors().getParamSpace().at(paramIndex).back().second;
						
						if(lowestParamValue > highestParamValue)
							swap(lowestParamValue, highestParamValue);

						// works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)			
						if(denom < 0) {
							if(isSucc && lowestParamValue <= paramValues.back()) {
								upperPositiveDirection = true;
								
								if( mostRightOneValue == numeric_limits<double>::max() ||
								(biggestConvexHullOfParamSubspace && mostRightOneValue < paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostRightOneValue > paramValues.back()) )
									mostRightOneValue = paramValues.back();
							}
							if(!isSucc && highestParamValue >= paramValues.back()) {
								upperNegativeDirection = true;
								
								if( mostLeftOneValue == numeric_limits<double>::lowest() ||
								(biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
									mostLeftOneValue = paramValues.back();
							}
						} else { // denom > 0
							if(!isSucc && lowestParamValue <= paramValues.back()) {
								upperNegativeDirection = true;
								
								if( mostRightOneValue == numeric_limits<double>::max() ||
								(biggestConvexHullOfParamSubspace && mostRightOneValue < paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostRightOneValue > paramValues.back()) )
									mostRightOneValue = paramValues.back();
							}
							if(isSucc && highestParamValue >= paramValues.back()) {
								upperPositiveDirection = true;
								
								if( mostLeftOneValue == numeric_limits<double>::lowest() ||
								(biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
								(!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
									mostLeftOneValue = paramValues.back();
							}
						}

/*					
						if(isSucc) {
					
							if(paramValues.back() < 0) {
								upperNegativeDirection = true;

							} else {
								upperPositiveDirection = true;

								if(paramValues.back() < oneParamValue)
									oneParamValue = paramValues.back();
							}
						} else {
						
							if(paramValues.back() < 0) {
								upperNegativeDirection = true;						
						
								if(oneParamValue > paramValues.back())
									oneParamValue = paramValues.back();
							} else {
								upperPositiveDirection = true;

							}
						} */
						
					} else {	// abs(denom) == 0 (ERGO: it might be at border of state space)
					//	cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
						if(derivationValue < 0) {
							upperNegativeDirection = true;
							
							if(!isSucc) {
								mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
								mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
							}
						} else {
							upperPositiveDirection = true;
							
							if(isSucc) {
								mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
								mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
							}
						} 						
					}
					
				} else {	// paramIndex == -1 (ERGO: no unknown parameter in equation)
				//	cerr << "derivation = " << derivationValue << endl;
					if(derivationValue < 0) {
						upperNegativeDirection = true;
					} else {
						upperPositiveDirection = true;
					} 
				}
			}
			
			//cerr << "most left  param value on upper facet: " << mostLeftOneValue << endl;
			//cerr << "most right param value on upper facet: " << mostRightOneValue << endl;
		
			if(s.getCoors().at(v) != dataModel.getThresholdsForVariable(v).size() -2) {		
				if(!isSucc) {
					//If I want predecessors of state 's'
				
					if(upperNegativeDirection) {
						//There exists edge from lower state to state 's'

						vector<size_t> newStateCoors(s.getCoors());
						newStateCoors.at(v) = newStateCoors.at(v) + 1;

						ParameterSpace newPS;
						if(paramIndex != -1) {
							//Parameter space needs to be cut for this edge
	//						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
							newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,mostLeftOneValue,mostRightOneValue);						
						} else {
							//Edge is for whole parameter space						
							newPS = ParameterSpace(s.getColors());
						}
				
						State newState(s.getDims(),newStateCoors,newPS);
					
						returnedStates.push_back(newState);
					}
				} else {
					//If I want successors of state 's'
				
					if(upperPositiveDirection) {
						//There exists edge from lower state to state 's'

						vector<size_t> newStateCoors(s.getCoors());
						newStateCoors.at(v) = newStateCoors.at(v) + 1;

						ParameterSpace newPS;
						if(paramIndex != -1) {
							//Parameter space needs to be cut for this edge
	//						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
							newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,mostLeftOneValue,mostRightOneValue);						
						} else {
							//Edge is for whole parameter space						
							newPS = ParameterSpace(s.getColors());
						}
				
						State newState(s.getDims(),newStateCoors,newPS);
					
						returnedStates.push_back(newState);
					}
				}
			}
		}
		
		if(hasSelfloop) {
			if(lowerPositiveDirection && upperPositiveDirection && !lowerNegativeDirection && !upperNegativeDirection ||
				!lowerPositiveDirection && !upperPositiveDirection && lowerNegativeDirection && upperNegativeDirection) {
				
				hasSelfloop = false;
			}
		}
	}
	
	if(hasSelfloop) {
		returnedStates.push_back(s);
	}
	
	return returnedStates;
}


vector<State> StateSpaceGenerator::initAP(string var, Operators op, double thres,const vector<pair<double,double> >& subspace) {
	size_t varIndex = dataModel.getVariableIndex(var);
	
	size_t thresIndex = 0;
	for(size_t i = 0; i < dataModel.getThresholdsForVariable(varIndex).size(); i++) {			
		if(dataModel.getThresholdsForVariable(varIndex).at(i) > thres) {
			switch(op) {
			
			case Operators::GR :	
			case Operators::LSEQ :				
				thresIndex = i;
				break;
			case Operators::LS :	
			case Operators::GREQ :				
				thresIndex = i-1;
				break;
			default:
				cerr << "Error: Unknown value of input parameter (Operators) in function \"initAP\"\n";
			}
			break;
		}
		if(dataModel.getThresholdsForVariable(varIndex).at(i) == thres) {
			thresIndex = i;
			break;
		}
	}
	cerr << "Threshold found\n";
	
	vector<State> result;
	
	switch(op) {
	
	case Operators::GREQ :
	case Operators::GR :
		result = getRightInitStates(varIndex, thresIndex, dataModel.getThresholdsForVariable(varIndex).size()-1, subspace);
		break;
		
	case Operators::LSEQ :
	case Operators::LS :
		result = getRightInitStates(varIndex, 0, thresIndex, subspace);		
		break;
		
	default:
		cerr << "Error: Unknown value of input parameter (Operators) in function \"initAP\"\n";
	}

	return result;
}


vector<State> StateSpaceGenerator::init() {
	vector<State> result;
	
	result = getRightInitStates(-1);
	return result;
}


double StateSpaceGenerator::value(const vector<size_t>& vertex, size_t dim, int& paramIndex, double& denom) {

	bool dbg = false;
	double sum = 0;
	
	paramIndex = -1;
	
	for(size_t s = 0; s < dataModel.getEquationForVariable(dim).size(); s++) {
	
		//adding value of constant in actual summember 's' of equation for variable 'dim'
		double underSum = dataModel.getSumForVarByIndex(dim,s).GetConstant();
		
		if(dbg) std::cerr << "\tconstant is " << underSum << "\n";

		//adding values of variables in actual summember 's' of equation for variable 'dim' in given point
		for(size_t v = 0; v < dataModel.getSumForVarByIndex( dim, s ).GetVars().size(); v++) {	
		
			size_t actualVarIndex = dataModel.getSumForVarByIndex(dim,s).GetVars().at(v) - 1;
			double thres = dataModel.getThresholdForVarByIndex( actualVarIndex, vertex.at(actualVarIndex) );
			
			if(dbg) std::cerr << "\tthres for var " << actualVarIndex << " is " << thres << "\n";
			
			underSum *= thres;
		}
		//cerr << "start of evalueting of ramps\n";
		//adding enumerated ramps for actual summember 's' of equation for variable 'dim'
		for(size_t r = 0; r < dataModel.getSumForVarByIndex(dim, s).GetRamps().size(); r++) {
			//cerr << "ramp: " << dataModel.getSumForVarByIndex(dim, s).GetRamps().at(r) << endl;
			size_t rampVarIndex = dataModel.getSumForVarByIndex(dim, s).GetRamps().at(r).dim -1;
			//cerr << "ramp var index: " << rampVarIndex << endl;
			double thres = dataModel.getThresholdForVarByIndex( rampVarIndex, vertex.at(rampVarIndex) );
			//cerr << "thres for this var: " << thres << endl;
			underSum *= dataModel.getSumForVarByIndex(dim, s).GetRamps().at(r).value(thres);
			//cerr << "local underSum = " << underSum << endl;
		}

		//adding enumerated step functions for actual summember 's' of equation for variable 'dim'
		for(size_t r = 0; r < dataModel.getSumForVarByIndex(dim, s).GetSteps().size(); r++) {
		
			size_t stepVarIndex = dataModel.getSumForVarByIndex(dim, s).GetSteps().at(r).dim -1;
			double thres = dataModel.getThresholdForVarByIndex( stepVarIndex, vertex.at(stepVarIndex) );
			
			underSum *= dataModel.getSumForVarByIndex(dim, s).GetSteps().at(r).value(thres);
		}
		
		/*
		//adding enumerated hill functions for actual summember 's' of equation for variable 'dim'
		for(size_t r = 0; r < dataModel.getSumForVarByIndex(dim, s).GetHills().size(); r++) {
		
			size_t hillVarIndex = dataModel.getSumForVarByIndex(dim, s).GetHills().at(r).dim -1;
			double thres = dataModel.getThresholdForVarByIndex( hillVarIndex, vertex.at(hillVarIndex) );
			
			underSum *= dataModel.getSumForVarByIndex(dim, s).GetHills().at(r).value(thres);
		}
		*/

		//adding average value of actual summember's parameter, if any exists
		if(dataModel.getSumForVarByIndex(dim,s).hasParam()) {
		
			if(!parametrized) {
				std::pair<double,double> param = dataModel.getParamRange(dataModel.getSumForVarByIndex(dim,s).GetParam() - 1);
			
				underSum *= (param.second + param.first) * 0.5;
				
				//adding enumerated summember 's' to sum
				sum += underSum;
				
			} else {
		
				paramIndex = dataModel.getSumForVarByIndex(dim,s).GetParam() - 1;
				denom += underSum;
			}
			
		} else {
		
			//adding enumerated summember 's' to sum
			sum += underSum;
		}
		

	}
	
	if ( dbg ) std::cerr << "final value = " << sum << std::endl;
	
	return sum;
}


void StateSpaceGenerator::getRightVerticesRecursive(const State& s, size_t dim, bool lower, vector<vector<size_t> >& vertices, vector<size_t>& coors, size_t actIndex) {
	
	if(actIndex == dataModel.getDims()) {
	
		vertices.push_back(coors);
		return;
	}

	if(actIndex == dim) {
		if(lower) {
			coors.at(actIndex) = s.getCoors().at(actIndex);
			getRightVerticesRecursive(s, dim, lower, vertices, coors, actIndex+1);
		} else {
			coors.at(actIndex) = s.getCoors().at(actIndex) + 1;
			getRightVerticesRecursive(s, dim, lower, vertices, coors, actIndex+1);		
		}
	} else {
	
		for(size_t i = s.getCoors().at(actIndex); i < s.getCoors().at(actIndex) + 2; ++i) {
			coors.at(actIndex) = i;
			getRightVerticesRecursive(s, dim, lower, vertices, coors, actIndex+1);
		}
	}
}



vector<vector<size_t> > StateSpaceGenerator::getRightVertices(const State& s, size_t dim, bool lower) {

	vector<size_t> coors(dataModel.getDims());
	vector<vector<size_t> > vertices;
	
	getRightVerticesRecursive(s, dim, lower, vertices, coors, 0);
	
	return vertices;
}


/*
void StateSpaceGenerator::getRightStatesRecursive(size_t begin, size_t end, size_t varIndex, vector<State>& states, vector<size_t>& coors, size_t actIndex) {

	const size_t dims = dataModel.getDims();
	
	if(actIndex == dims) {
		ParameterSpace ps(dataModel.getParamSize(),dataModel.getParamRanges());
		State current(dims,coors,ps);
		states.push_back(current);
		return;
	}

	if(actIndex == varIndex) {
		for(int i = begin; i < end; i++) {
			coors.at(actIndex) = i;
			getRightStatesRecursive(begin, end, varIndex, states, coors, actIndex+1);
		}
	} else {
		for(int i = 0; i < dataModel.getThresholdsForVariable(actIndex).size(); ++i) {
			coors.at(actIndex) = i;			
			getRightStatesRecursive(begin, end, varIndex, states, coors, actIndex+1);
		}
	}
}
*/

void StateSpaceGenerator::getRightStatesRecursive(vector<pair<size_t,size_t> >& thresholdsIndexis, vector<State>& states, vector<size_t>& coors, size_t actIndex) {

	const size_t dims = dataModel.getDims();
	
	if(actIndex == dims) {
	
		ParameterSpace ps(dataModel.getParamSize(),dataModel.getParamRanges());
		State current(dims,coors,ps);
		states.push_back(current);
		
		return;
	}

	for(int i = thresholdsIndexis.at(actIndex).first; i < thresholdsIndexis.at(actIndex).second; ++i) {
		coors.at(actIndex) = i;
		getRightStatesRecursive(thresholdsIndexis, states, coors, actIndex+1);
	}
}


vector<State> StateSpaceGenerator::getRightInitStates(int varIndex, size_t begin, size_t end, const vector<pair<double,double> >& subspace) {
	
	vector<size_t> coors(dataModel.getDims());
	vector<pair<size_t,size_t> > thresholdsIndexis(dataModel.getDims());
	
	vector<State> ss;
	
	if(varIndex != -1) {
		for(int i = 0; i < dataModel.getDims(); i++) {
			if(varIndex == i) {
				
				if(subspace.at(i).second < begin || subspace.at(i).first > end)
					return ss;
			
				if(subspace.at(i).first < begin)
					thresholdsIndexis.at(i).first = begin;
				else
					thresholdsIndexis.at(i).first = subspace.at(i).first;
					
				if(subspace.at(i).second > end)
					thresholdsIndexis.at(i).second = end;
				else
					thresholdsIndexis.at(i).second = subspace.at(i).second;
			} else {
				thresholdsIndexis.at(i).first = subspace.at(i).first;//0;
				thresholdsIndexis.at(i).second = subspace.at(i).second;//dataModel.getThresholdsForVariable(i).size() - 1;
			}
		}
	} else {
		for(int i = 0; i < dataModel.getDims(); i++) {
			thresholdsIndexis.at(i).first = dataModel.getThresholdIndexForInitValueOfVar(dataModel.getInitsValuesForVariable(i).first, i);
			thresholdsIndexis.at(i).second = dataModel.getThresholdIndexForInitValueOfVar(dataModel.getInitsValuesForVariable(i).second, i);
			//cout << "Inits for " << i << ": " << thresholdsIndexis.at(i).first << " - " << thresholdsIndexis.at(i).second << "\n";
		}
	}
		
	getRightStatesRecursive(thresholdsIndexis, ss, coors, 0);
	
	return ss;
}
