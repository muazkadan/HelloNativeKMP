#include <cstring>
#include "native_greeting.h"

extern "C" {
    const char* getNativeGreeting() {
        static const char* greeting = "Hello from C++";
        return greeting;
    }
}
