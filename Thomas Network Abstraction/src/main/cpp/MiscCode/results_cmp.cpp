#include <fstream>
#include <iostream>
#include <regex>
#include <string>

using namespace std;

int main() {
   fstream old_file("D:/SYBILA/Coloring/Auxiliary/Lytic_compare/old.out", ios::in); string old_data;
   fstream new_file("D:/SYBILA/Coloring/Auxiliary/Lytic_compare/new.out", ios::in); string new_data;
   fstream unknown("D:/SYBILA/Coloring/Auxiliary/Lytic_compare/missing.out", ios::out);
   std::string temp;

   cout << "Read old." << endl;
   while (std::getline(old_file, temp))
      old_data.append(temp).append("\n");

   cout << "Read new." << endl;
   while (std::getline(new_file, temp))
      new_data.append(temp).append("\n");


   cout << "Try matches." << endl;
   regex param("(\\d,){23}\\d");
   smatch matching;
   std::size_t count = 0;
   while (std::regex_search(old_data, matching, param)) {
      if (!std::regex_search(new_data, regex(matching.str(0)))) {
         unknown << matching.str(0);
         cout << matching.str(0);
      }
      old_data = matching.suffix().str();
   }

   return 0;
}
