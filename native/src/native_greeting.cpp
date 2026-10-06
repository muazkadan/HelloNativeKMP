#include "native_greeting.h"

extern "C" const char* getNativeGreeting(void) {
    return "Hello from C++";
}
