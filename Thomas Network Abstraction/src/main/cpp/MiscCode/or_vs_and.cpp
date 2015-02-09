// Clocks - dependendent on the achitecture. 
#ifdef __GNUC__
#include <sys/time.h>
/**
 * @return	time in miliseconds
 */
long long my_clock() {
	timeval tv;
	gettimeofday(&tv, 0);
	return tv.tv_sec*1000 + tv.tv_usec/1000;
}
#else
#include <windows.h>
/**
 * @return	time in miliseconds
 */
long long my_clock() {
	return GetTickCount();
}
#endif

#include <iostream>

int main() {
	unsigned int iterated1, iterated2, iterated3, iterated4, iterated5;
	iterated1 = iterated2 = iterated3 = iterated4 = iterated5 = 0;
	long long time = my_clock();
	const int cycles = 100000000;
	
	for (std::size_t i = 0; i < cycles; i++) {
		iterated1++; iterated1 <<= 1;
		iterated2++; iterated2 <<= 1;
		iterated3++; iterated3 <<= 1;
		iterated4++; iterated4 <<= 1;
		iterated5++; iterated5 <<= 1;
	}
	std::cout << iterated1 << " " << iterated2 << " " << iterated3 << " " << iterated4 << " " << iterated5 << std::endl;
	std::cout << "Add time in " << cycles << " cycles is: " << my_clock() - time << std::endl;
	
	iterated1 = iterated2 = iterated3 = iterated4 = iterated5 = 0;
	time = my_clock();
	for (std::size_t i = 0; i < cycles; i++) {
		iterated1 |= 1; iterated1 <<= 1;
		iterated2 |= 1; iterated2 <<= 1;
		iterated3 |= 1; iterated3 <<= 1;
		iterated4 |= 1; iterated4 <<= 1;
		iterated5 |= 1; iterated5 <<= 1;
	}
	std::cout << iterated1 << " " << iterated2 << " " << iterated3 << " " << iterated4 << " " << iterated5 << std::endl;
	std::cout << "Or time in " << cycles << " cycles is: " << my_clock() - time << std::endl;
}