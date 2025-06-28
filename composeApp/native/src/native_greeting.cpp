#include <cstring>
#include <iostream>

extern "C" {
    const char* getNativeGreeting() {
        static const char* greeting = "Hello from C++";
        return greeting;
    }
}

int main() {
    std::cout << getNativeGreeting() << std::endl;
    return 0;
}