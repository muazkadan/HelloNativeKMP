//! Plain C API declared in `include/native_greeting.h`. Kotlin/Native links it statically through
//! cinterop. It's a separate crate so the JNI library doesn't re-export these symbols.

use std::ffi::c_char;

/// Returns a greeting with static storage duration; callers must not free it.
#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "C" fn getNativeGreeting() -> *const c_char {
    native_greeting_core::greeting().as_ptr()
}
