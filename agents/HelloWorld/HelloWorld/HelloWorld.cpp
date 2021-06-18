// HelloWorld.cpp : This file contains the 'main' function. Program execution begins and ends there.
//

#include <iostream>

#include <windows.h>

int main()
{
	std::cout << "Hello World!" << std::endl;

	// Remove previous file contents
	FILE* data_file = fopen("test.dat", "w");

	fputs("This is a message. It means something", data_file);

	fclose(data_file);

	std::cout << "File written!" << std::endl;
	while (1) {

	}
	return 1;
}

int APIENTRY WinMain(HINSTANCE hInstance,
	HINSTANCE hPrevInstance,
	LPSTR     lpCmdLine,
	int       nCmdShow)
{
	main();

	return 0;
}



