//! JNI bridge shared by Android and desktop JVM. Methods are bound with `RegisterNatives` in
//! `JNI_OnLoad` instead of relying on mangled symbol names, so only `JNI_OnLoad` is exported
//! from the shared library.

use std::ffi::c_void;

use jni::errors::Result;
use jni::objects::{JClass, JString};
use jni::strings::JNIStr;
use jni::sys::{JNI_ERR, JNI_VERSION_1_6, jint};
use jni::{Env, JavaVM, NativeMethod, jni_str, native_method};

const BINDING_CLASS: &JNIStr = jni_str!("dev/muazkadan/hellonative/nativeinterop/NativeGreetingJni");

// native_method! derives each Java name and signature from the Rust declaration, and wraps the
// implementation so errors and panics become Java exceptions.
const METHODS: &[NativeMethod] = &[native_method! {
    static fn get_native_greeting() -> JString,
}];

fn get_native_greeting<'local>(env: &mut Env<'local>, _class: JClass<'local>) -> Result<JString<'local>> {
    JString::from_str(env, native_greeting_core::greeting().to_string_lossy())
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(vm: *mut jni::sys::JavaVM, _reserved: *mut c_void) -> jint {
    // Safety: the JVM passes a valid JavaVM pointer.
    let vm = unsafe { JavaVM::from_raw(vm) };
    // Safety: every entry in METHODS was type-checked against its signature by native_method!.
    let registered = vm.attach_current_thread(|env| unsafe { env.register_native_methods(BINDING_CLASS, METHODS) });
    if registered.is_ok() { JNI_VERSION_1_6 } else { JNI_ERR }
}
