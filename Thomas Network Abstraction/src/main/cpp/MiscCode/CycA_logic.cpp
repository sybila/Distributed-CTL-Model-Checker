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
	return (!v[0] & !v[3] & !(v[4] & v[5]) & (v[1] | v[2]));
}

int main() {
	vector<int> values(6,0);
	for (std::size_t vec_num = 0; vec_num < pow(2.0, 6.0); vec_num++) {
		cout << "\t<REGUL mask=\"";
		
		for (size_t val_num = 0; val_num < values.size(); val_num++) 
			cout << values[val_num];
			
		cout << "\" t_value=\"";
		
		cout << test(values);
		
		cout << "\" />\n";	
		
		iterate_val(values);
	}
}