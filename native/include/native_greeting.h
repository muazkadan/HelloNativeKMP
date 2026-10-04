#ifndef NATIVE_GREETING_H
#define NATIVE_GREETING_H

#ifdef __cplusplus
extern "C" {
#endif

/// Returns a greeting with static storage duration; callers must not free it.
const char* getNativeGreeting(void);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_GREETING_H
