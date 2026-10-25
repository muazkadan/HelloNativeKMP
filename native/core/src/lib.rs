//! Platform-independent core, shared by the C API (`c/`) and the JNI bridge (`jni/`).

use std::ffi::CStr;

/// Returns the greeting. It's a C string so the C API can hand it out without copying.
pub fn greeting() -> &'static CStr {
    c"Hello from Rust"
}
