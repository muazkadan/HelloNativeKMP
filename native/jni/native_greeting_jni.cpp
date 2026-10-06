#include <jni.h>

#include <iterator>

#include "native_greeting.h"

// JNI bridge for Android (desktop uses the Java FFM API instead). Methods are bound with
// RegisterNatives in JNI_OnLoad instead of relying on mangled symbol names,
// so only JNI_OnLoad is exported from the shared library.

namespace {

constexpr const char* kBindingClass = "dev/muazkadan/hellonative/nativeinterop/NativeGreetingJni";

jstring GetNativeGreeting(JNIEnv* env, jclass /* clazz */) {
    return env->NewStringUTF(getNativeGreeting());
}

JNINativeMethod kMethods[] = {
    {const_cast<char*>("getNativeGreeting"), const_cast<char*>("()Ljava/lang/String;"),
     reinterpret_cast<void*>(GetNativeGreeting)},
};

} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass(kBindingClass);
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    const jint result = env->RegisterNatives(clazz, kMethods, static_cast<jint>(std::size(kMethods)));
    env->DeleteLocalRef(clazz);

    return result == JNI_OK ? JNI_VERSION_1_6 : JNI_ERR;
}
