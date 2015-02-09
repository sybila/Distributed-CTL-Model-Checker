#include <iostream>
#include <cmath>
#include <string>
#include <vector>

using namespace std;

void iterate_val(vector<int> & values) {
	for (size_t val_num = 0; val_num < values.size(); val_num++) {
		if (values[val_num] == 0) {
			values[val_num] = 1;
			break;
		}
		else 
			values[val_num] = 0;
	}
}

bool test(vector<int> & v) {
	return (!v[2] | (v[3] & (v[1] | v[0] | v[4])));
}

int main() {
	vector<int> values(5,0);
	for (std::size_t vec_num = 0; vec_num < pow(2.0, 5.0); vec_num++) {
		cout << "\t<REGUL mask=\"";
		
		for (size_t val_num = 0; val_num < values.size(); val_num++) 
			cout << values[val_num];
			
		cout << "\" t_value=\"";
		
		cout << test(values);
		
		cout << "\" />\n";	
		
		iterate_val(values);
	}
}