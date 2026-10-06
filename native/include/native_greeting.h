#ifndef NATIVE_GREETING_H
#define NATIVE_GREETING_H

// Exported only when built as a shared library (desktop, loaded through the Java FFM API).
// Static builds keep hidden visibility so the JNI bridge exports nothing but JNI_OnLoad.
#if defined(NATIVE_GREETING_EXPORTS)
#  if defined(_WIN32)
#    define NATIVE_GREETING_API __declspec(dllexport)
#  else
#    define NATIVE_GREETING_API __attribute__((visibility("default")))
#  endif
#else
#  define NATIVE_GREETING_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/// Returns a greeting with static storage duration; callers must not free it.
NATIVE_GREETING_API const char* getNativeGreeting(void);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_GREETING_H
