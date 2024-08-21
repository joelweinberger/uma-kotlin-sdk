// This file was autogenerated by some hot garbage in the `uniffi` crate.
// Trust me, you don't want to mess with it!

@file:Suppress("NAME_SHADOWING")

package me.uma.crypto.internal

// Common helper code.
//
// Ideally this would live in a separate .kt file where it can be unittested etc
// in isolation, and perhaps even published as a re-useable package.
//
// However, it's important that the detils of how this helper code works (e.g. the
// way that different builtin types are passed across the FFI) exactly match what's
// expected by the Rust code on the other side of the interface. In practice right
// now that means coming from the exact some version of `uniffi` that was used to
// compile the Rust component. The easiest way to ensure this is to bundle the Kotlin
// helpers directly inline like we're doing here.

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.ByReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// This is a helper for safely working with byte buffers returned from the Rust code.
// A rust-owned buffer is represented by its capacity, its current length, and a
// pointer to the underlying data.

@Structure.FieldOrder("capacity", "len", "data")
open class RustBuffer : Structure() {
    @JvmField var capacity: Int = 0

    @JvmField var len: Int = 0

    @JvmField var data: Pointer? = null

    class ByValue :
        RustBuffer(),
        Structure.ByValue

    class ByReference :
        RustBuffer(),
        Structure.ByReference

    companion object {
        internal fun alloc(size: Int = 0) =
            rustCall { status ->
                _UniFFILib.INSTANCE.ffi_uma_crypto_338f_rustbuffer_alloc(size, status).also {
                    if (it.data == null) {
                        throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=$size)")
                    }
                }
            }

        internal fun free(buf: RustBuffer.ByValue) =
            rustCall { status ->
                _UniFFILib.INSTANCE.ffi_uma_crypto_338f_rustbuffer_free(buf, status)
            }
    }

    @Suppress("TooGenericExceptionThrown")
    fun asByteBuffer() =
        this.data?.getByteBuffer(0, this.len.toLong())?.also {
            it.order(ByteOrder.BIG_ENDIAN)
        }
}

/**
 * The equivalent of the `*mut RustBuffer` type.
 * Required for callbacks taking in an out pointer.
 *
 * Size is the sum of all values in the struct.
 */
class RustBufferByReference : ByReference(16) {
    /**
     * Set the pointed-to `RustBuffer` to the given value.
     */
    fun setValue(value: RustBuffer.ByValue) {
        // NOTE: The offsets are as they are in the C-like struct.
        val pointer = getPointer()
        pointer.setInt(0, value.capacity)
        pointer.setInt(4, value.len)
        pointer.setPointer(8, value.data)
    }
}

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
open class ForeignBytes : Structure() {
    @JvmField var len: Int = 0

    @JvmField var data: Pointer? = null

    class ByValue :
        ForeignBytes(),
        Structure.ByValue
}

// The FfiConverter interface handles converter types to and from the FFI
//
// All implementing objects should be public to support external types.  When a
// type is external we need to import it's FfiConverter.
public interface FfiConverter<KotlinType, FfiType> {
    // Convert an FFI type to a Kotlin type
    fun lift(value: FfiType): KotlinType

    // Convert an Kotlin type to an FFI type
    fun lower(value: KotlinType): FfiType

    // Read a Kotlin type from a `ByteBuffer`
    fun read(buf: ByteBuffer): KotlinType

    // Calculate bytes to allocate when creating a `RustBuffer`
    //
    // This must return at least as many bytes as the write() function will
    // write. It can return more bytes than needed, for example when writing
    // Strings we can't know the exact bytes needed until we the UTF-8
    // encoding, so we pessimistically allocate the largest size possible (3
    // bytes per codepoint).  Allocating extra bytes is not really a big deal
    // because the `RustBuffer` is short-lived.
    fun allocationSize(value: KotlinType): Int

    // Write a Kotlin type to a `ByteBuffer`
    fun write(
        value: KotlinType,
        buf: ByteBuffer,
    )

    // Lower a value into a `RustBuffer`
    //
    // This method lowers a value into a `RustBuffer` rather than the normal
    // FfiType.  It's used by the callback interface code.  Callback interface
    // returns are always serialized into a `RustBuffer` regardless of their
    // normal FFI type.
    fun lowerIntoRustBuffer(value: KotlinType): RustBuffer.ByValue {
        val rbuf = RustBuffer.alloc(allocationSize(value))
        try {
            val bbuf =
                rbuf.data!!.getByteBuffer(0, rbuf.capacity.toLong()).also {
                    it.order(ByteOrder.BIG_ENDIAN)
                }
            write(value, bbuf)
            rbuf.writeField("len", bbuf.position())
            return rbuf
        } catch (e: Throwable) {
            RustBuffer.free(rbuf)
            throw e
        }
    }

    // Lift a value from a `RustBuffer`.
    //
    // This here mostly because of the symmetry with `lowerIntoRustBuffer()`.
    // It's currently only used by the `FfiConverterRustBuffer` class below.
    fun liftFromRustBuffer(rbuf: RustBuffer.ByValue): KotlinType {
        val byteBuf = rbuf.asByteBuffer()!!
        try {
            val item = read(byteBuf)
            if (byteBuf.hasRemaining()) {
                throw RuntimeException("junk remaining in buffer after lifting, something is very wrong!!")
            }
            return item
        } finally {
            RustBuffer.free(rbuf)
        }
    }
}

// FfiConverter that uses `RustBuffer` as the FfiType
public interface FfiConverterRustBuffer<KotlinType> : FfiConverter<KotlinType, RustBuffer.ByValue> {
    override fun lift(value: RustBuffer.ByValue) = liftFromRustBuffer(value)

    override fun lower(value: KotlinType) = lowerIntoRustBuffer(value)
}

// A handful of classes and functions to support the generated data structures.
// This would be a good candidate for isolating in its own ffi-support lib.
// Error runtime.
@Structure.FieldOrder("code", "error_buf")
internal open class RustCallStatus : Structure() {
    @JvmField var code: Int = 0

    @JvmField var error_buf: RustBuffer.ByValue = RustBuffer.ByValue()

    fun isSuccess(): Boolean = code == 0

    fun isError(): Boolean = code == 1

    fun isPanic(): Boolean = code == 2
}

class InternalException(
    message: String,
) : Exception(message)

// Each top-level error class has a companion object that can lift the error from the call status's rust buffer
interface CallStatusErrorHandler<E> {
    fun lift(error_buf: RustBuffer.ByValue): E
}

// Helpers for calling Rust
// In practice we usually need to be synchronized to call this safely, so it doesn't
// synchronize itself

// Call a rust function that returns a Result<>.  Pass in the Error class companion that corresponds to the Err
private inline fun <U, E : Exception> rustCallWithError(
    errorHandler: CallStatusErrorHandler<E>,
    callback: (RustCallStatus) -> U,
): U {
    var status = RustCallStatus()
    val return_value = callback(status)
    if (status.isSuccess()) {
        return return_value
    } else if (status.isError()) {
        throw errorHandler.lift(status.error_buf)
    } else if (status.isPanic()) {
        // when the rust code sees a panic, it tries to construct a rustbuffer
        // with the message.  but if that code panics, then it just sends back
        // an empty buffer.
        if (status.error_buf.len > 0) {
            throw InternalException(FfiConverterString.lift(status.error_buf))
        } else {
            throw InternalException("Rust panic")
        }
    } else {
        throw InternalException("Unknown rust call status: $status.code")
    }
}

// CallStatusErrorHandler implementation for times when we don't expect a CALL_ERROR
object NullCallStatusErrorHandler : CallStatusErrorHandler<InternalException> {
    override fun lift(error_buf: RustBuffer.ByValue): InternalException {
        RustBuffer.free(error_buf)
        return InternalException("Unexpected CALL_ERROR")
    }
}

// Call a rust function that returns a plain value
private inline fun <U> rustCall(callback: (RustCallStatus) -> U): U = rustCallWithError(NullCallStatusErrorHandler, callback)

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
@Synchronized
private fun findLibraryName(componentName: String): String {
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "uniffi_uma_crypto"
}

private inline fun <reified Lib : Library> loadIndirect(componentName: String): Lib =
    Native.load<Lib>(findLibraryName(componentName), Lib::class.java)

// A JNA Library to expose the extern-C FFI definitions.
// This is an implementation detail which will be called internally by the public API.

internal interface _UniFFILib : Library {
    companion object {
        internal val INSTANCE: _UniFFILib by lazy {
            loadIndirect<_UniFFILib>(componentName = "uma_crypto")
        }
    }

    fun ffi_uma_crypto_338f_KeyPair_object_free(
        `ptr`: Pointer,
        _uniffi_out_err: RustCallStatus,
    ): Unit

    fun uma_crypto_338f_KeyPair_get_public_key(
        `ptr`: Pointer,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_KeyPair_get_private_key(
        `ptr`: Pointer,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_sign_ecdsa(
        `msg`: RustBuffer.ByValue,
        `privateKeyBytes`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_verify_ecdsa(
        `msg`: RustBuffer.ByValue,
        `signatureBytes`: RustBuffer.ByValue,
        `publicKeyBytes`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): Byte

    fun uma_crypto_338f_encrypt_ecies(
        `msg`: RustBuffer.ByValue,
        `publicKeyBytes`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_decrypt_ecies(
        `cipherText`: RustBuffer.ByValue,
        `privateKeyBytes`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_generate_keypair(_uniffi_out_err: RustCallStatus): Pointer

    fun uma_crypto_338f_encode_bech32(
        `hrp`: RustBuffer.ByValue,
        `messageData`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun uma_crypto_338f_decode_bech32(
        `bech32Str`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun ffi_uma_crypto_338f_rustbuffer_alloc(
        `size`: Int,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun ffi_uma_crypto_338f_rustbuffer_from_bytes(
        `bytes`: ForeignBytes.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue

    fun ffi_uma_crypto_338f_rustbuffer_free(
        `buf`: RustBuffer.ByValue,
        _uniffi_out_err: RustCallStatus,
    ): Unit

    fun ffi_uma_crypto_338f_rustbuffer_reserve(
        `buf`: RustBuffer.ByValue,
        `additional`: Int,
        _uniffi_out_err: RustCallStatus,
    ): RustBuffer.ByValue
}

// Public interface members begin here.

public object FfiConverterUByte : FfiConverter<UByte, Byte> {
    override fun lift(value: Byte): UByte = value.toUByte()

    override fun read(buf: ByteBuffer): UByte = lift(buf.get())

    override fun lower(value: UByte): Byte = value.toByte()

    override fun allocationSize(value: UByte) = 1

    override fun write(
        value: UByte,
        buf: ByteBuffer,
    ) {
        buf.put(value.toByte())
    }
}

public object FfiConverterBoolean : FfiConverter<Boolean, Byte> {
    override fun lift(value: Byte): Boolean = value.toInt() != 0

    override fun read(buf: ByteBuffer): Boolean = lift(buf.get())

    override fun lower(value: Boolean): Byte = if (value) 1.toByte() else 0.toByte()

    override fun allocationSize(value: Boolean) = 1

    override fun write(
        value: Boolean,
        buf: ByteBuffer,
    ) {
        buf.put(lower(value))
    }
}

public object FfiConverterString : FfiConverter<String, RustBuffer.ByValue> {
    // Note: we don't inherit from FfiConverterRustBuffer, because we use a
    // special encoding when lowering/lifting.  We can use `RustBuffer.len` to
    // store our length and avoid writing it out to the buffer.
    override fun lift(value: RustBuffer.ByValue): String {
        try {
            val byteArr = ByteArray(value.len)
            value.asByteBuffer()!!.get(byteArr)
            return byteArr.toString(Charsets.UTF_8)
        } finally {
            RustBuffer.free(value)
        }
    }

    override fun read(buf: ByteBuffer): String {
        val len = buf.getInt()
        val byteArr = ByteArray(len)
        buf.get(byteArr)
        return byteArr.toString(Charsets.UTF_8)
    }

    override fun lower(value: String): RustBuffer.ByValue {
        val byteArr = value.toByteArray(Charsets.UTF_8)
        // Ideally we'd pass these bytes to `ffi_bytebuffer_from_bytes`, but doing so would require us
        // to copy them into a JNA `Memory`. So we might as well directly copy them into a `RustBuffer`.
        val rbuf = RustBuffer.alloc(byteArr.size)
        rbuf.asByteBuffer()!!.put(byteArr)
        return rbuf
    }

    // We aren't sure exactly how many bytes our string will be once it's UTF-8
    // encoded.  Allocate 3 bytes per unicode codepoint which will always be
    // enough.
    override fun allocationSize(value: String): Int {
        val sizeForLength = 4
        val sizeForString = value.length * 3
        return sizeForLength + sizeForString
    }

    override fun write(
        value: String,
        buf: ByteBuffer,
    ) {
        val byteArr = value.toByteArray(Charsets.UTF_8)
        buf.putInt(byteArr.size)
        buf.put(byteArr)
    }
}

// Interface implemented by anything that can contain an object reference.
//
// Such types expose a `destroy()` method that must be called to cleanly
// dispose of the contained objects. Failure to call this method may result
// in memory leaks.
//
// The easiest way to ensure this method is called is to use the `.use`
// helper method to execute a block and destroy the object at the end.
interface Disposable {
    fun destroy()

    companion object {
        fun destroy(vararg args: Any?) {
            args
                .filterIsInstance<Disposable>()
                .forEach(Disposable::destroy)
        }
    }
}

inline fun <T : Disposable?, R> T.use(block: (T) -> R) =
    try {
        block(this)
    } finally {
        try {
            // N.B. our implementation is on the nullable type `Disposable?`.
            this?.destroy()
        } catch (e: Throwable) {
            // swallow
        }
    }

// The base class for all UniFFI Object types.
//
// This class provides core operations for working with the Rust `Arc<T>` pointer to
// the live Rust struct on the other side of the FFI.
//
// There's some subtlety here, because we have to be careful not to operate on a Rust
// struct after it has been dropped, and because we must expose a public API for freeing
// the Kotlin wrapper object in lieu of reliable finalizers. The core requirements are:
//
//   * Each `FFIObject` instance holds an opaque pointer to the underlying Rust struct.
//     Method calls need to read this pointer from the object's state and pass it in to
//     the Rust FFI.
//
//   * When an `FFIObject` is no longer needed, its pointer should be passed to a
//     special destructor function provided by the Rust FFI, which will drop the
//     underlying Rust struct.
//
//   * Given an `FFIObject` instance, calling code is expected to call the special
//     `destroy` method in order to free it after use, either by calling it explicitly
//     or by using a higher-level helper like the `use` method. Failing to do so will
//     leak the underlying Rust struct.
//
//   * We can't assume that calling code will do the right thing, and must be prepared
//     to handle Kotlin method calls executing concurrently with or even after a call to
//     `destroy`, and to handle multiple (possibly concurrent!) calls to `destroy`.
//
//   * We must never allow Rust code to operate on the underlying Rust struct after
//     the destructor has been called, and must never call the destructor more than once.
//     Doing so may trigger memory unsafety.
//
// If we try to implement this with mutual exclusion on access to the pointer, there is the
// possibility of a race between a method call and a concurrent call to `destroy`:
//
//    * Thread A starts a method call, reads the value of the pointer, but is interrupted
//      before it can pass the pointer over the FFI to Rust.
//    * Thread B calls `destroy` and frees the underlying Rust struct.
//    * Thread A resumes, passing the already-read pointer value to Rust and triggering
//      a use-after-free.
//
// One possible solution would be to use a `ReadWriteLock`, with each method call taking
// a read lock (and thus allowed to run concurrently) and the special `destroy` method
// taking a write lock (and thus blocking on live method calls). However, we aim not to
// generate methods with any hidden blocking semantics, and a `destroy` method that might
// block if called incorrectly seems to meet that bar.
//
// So, we achieve our goals by giving each `FFIObject` an associated `AtomicLong` counter to track
// the number of in-flight method calls, and an `AtomicBoolean` flag to indicate whether `destroy`
// has been called. These are updated according to the following rules:
//
//    * The initial value of the counter is 1, indicating a live object with no in-flight calls.
//      The initial value for the flag is false.
//
//    * At the start of each method call, we atomically check the counter.
//      If it is 0 then the underlying Rust struct has already been destroyed and the call is aborted.
//      If it is nonzero them we atomically increment it by 1 and proceed with the method call.
//
//    * At the end of each method call, we atomically decrement and check the counter.
//      If it has reached zero then we destroy the underlying Rust struct.
//
//    * When `destroy` is called, we atomically flip the flag from false to true.
//      If the flag was already true we silently fail.
//      Otherwise we atomically decrement and check the counter.
//      If it has reached zero then we destroy the underlying Rust struct.
//
// Astute readers may observe that this all sounds very similar to the way that Rust's `Arc<T>` works,
// and indeed it is, with the addition of a flag to guard against multiple calls to `destroy`.
//
// The overall effect is that the underlying Rust struct is destroyed only when `destroy` has been
// called *and* all in-flight method calls have completed, avoiding violating any of the expectations
// of the underlying Rust code.
//
// In the future we may be able to replace some of this with automatic finalization logic, such as using
// the new "Cleaner" functionaility in Java 9. The above scheme has been designed to work even if `destroy` is
// invoked by garbage-collection machinery rather than by calling code (which by the way, it's apparently also
// possible for the JVM to finalize an object while there is an in-flight call to one of its methods [1],
// so there would still be some complexity here).
//
// Sigh...all of this for want of a robust finalization mechanism.
//
// [1] https://stackoverflow.com/questions/24376768/can-java-finalize-an-object-when-it-is-still-in-scope/24380219
//
abstract class FFIObject(
    protected val pointer: Pointer,
) : Disposable,
    AutoCloseable {
    private val wasDestroyed = AtomicBoolean(false)
    private val callCounter = AtomicLong(1)

    protected open fun freeRustArcPtr() {
        // To be overridden in subclasses.
    }

    override fun destroy() {
        // Only allow a single call to this method.
        // TODO: maybe we should log a warning if called more than once?
        if (this.wasDestroyed.compareAndSet(false, true)) {
            // This decrement always matches the initial count of 1 given at creation time.
            if (this.callCounter.decrementAndGet() == 0L) {
                this.freeRustArcPtr()
            }
        }
    }

    @Synchronized
    override fun close() {
        this.destroy()
    }

    internal inline fun <R> callWithPointer(block: (ptr: Pointer) -> R): R {
        // Check and increment the call counter, to keep the object alive.
        // This needs a compare-and-set retry loop in case of concurrent updates.
        do {
            val c = this.callCounter.get()
            if (c == 0L) {
                throw IllegalStateException("${this.javaClass.simpleName} object has already been destroyed")
            }
            if (c == Long.MAX_VALUE) {
                throw IllegalStateException("${this.javaClass.simpleName} call counter would overflow")
            }
        } while (!this.callCounter.compareAndSet(c, c + 1L))
        // Now we can safely do the method call without the pointer being freed concurrently.
        try {
            return block(this.pointer)
        } finally {
            // This decrement always matches the increment we performed above.
            if (this.callCounter.decrementAndGet() == 0L) {
                this.freeRustArcPtr()
            }
        }
    }
}

public interface KeyPairInterface {
    fun `getPublicKey`(): List<UByte>

    fun `getPrivateKey`(): List<UByte>
}

class KeyPair(
    pointer: Pointer,
) : FFIObject(pointer),
    KeyPairInterface {
    /**
     * Disconnect the object from the underlying Rust object.
     *
     * It can be called more than once, but once called, interacting with the object
     * causes an `IllegalStateException`.
     *
     * Clients **must** call this method once done with the object, or cause a memory leak.
     */
    protected override fun freeRustArcPtr() {
        rustCall { status ->
            _UniFFILib.INSTANCE.ffi_uma_crypto_338f_KeyPair_object_free(this.pointer, status)
        }
    }

    override fun `getPublicKey`(): List<UByte> =
        callWithPointer {
            rustCall { _status ->
                _UniFFILib.INSTANCE.uma_crypto_338f_KeyPair_get_public_key(it, _status)
            }
        }.let {
            FfiConverterSequenceUByte.lift(it)
        }

    override fun `getPrivateKey`(): List<UByte> =
        callWithPointer {
            rustCall { _status ->
                _UniFFILib.INSTANCE.uma_crypto_338f_KeyPair_get_private_key(it, _status)
            }
        }.let {
            FfiConverterSequenceUByte.lift(it)
        }
}

public object FfiConverterTypeKeyPair : FfiConverter<KeyPair, Pointer> {
    override fun lower(value: KeyPair): Pointer = value.callWithPointer { it }

    override fun lift(value: Pointer): KeyPair = KeyPair(value)

    override fun read(buf: ByteBuffer): KeyPair {
        // The Rust code always writes pointers as 8 bytes, and will
        // fail to compile if they don't fit.
        return lift(Pointer(buf.getLong()))
    }

    override fun allocationSize(value: KeyPair) = 8

    override fun write(
        value: KeyPair,
        buf: ByteBuffer,
    ) {
        // The Rust code always expects pointers written as 8 bytes,
        // and will fail to compile if they don't fit.
        buf.putLong(Pointer.nativeValue(lower(value)))
    }
}

data class Bech32Data(
    var `data`: List<UByte>,
    var `hrp`: String,
)

public object FfiConverterTypeBech32Data : FfiConverterRustBuffer<Bech32Data> {
    override fun read(buf: ByteBuffer): Bech32Data =
        Bech32Data(
            FfiConverterSequenceUByte.read(buf),
            FfiConverterString.read(buf),
        )

    override fun allocationSize(value: Bech32Data) =
        (
            FfiConverterSequenceUByte.allocationSize(value.`data`) +
                FfiConverterString.allocationSize(value.`hrp`)
            )

    override fun write(
        value: Bech32Data,
        buf: ByteBuffer,
    ) {
        FfiConverterSequenceUByte.write(value.`data`, buf)
        FfiConverterString.write(value.`hrp`, buf)
    }
}

sealed class Bech32Exception(
    message: String,
) : Exception(message) {
    // Each variant is a nested class
    // Flat enums carries a string error message, so no special implementation is necessary.
    class Bech32EncodeException(
        message: String,
    ) : Bech32Exception(message)

    class Bech32DecodeException(
        message: String,
    ) : Bech32Exception(message)

    companion object ErrorHandler : CallStatusErrorHandler<Bech32Exception> {
        override fun lift(error_buf: RustBuffer.ByValue): Bech32Exception = FfiConverterTypeBech32Error.lift(error_buf)
    }
}

public object FfiConverterTypeBech32Error : FfiConverterRustBuffer<Bech32Exception> {
    override fun read(buf: ByteBuffer): Bech32Exception =
        when (buf.getInt()) {
            1 -> Bech32Exception.Bech32EncodeException(FfiConverterString.read(buf))
            2 -> Bech32Exception.Bech32DecodeException(FfiConverterString.read(buf))
            else -> throw RuntimeException("invalid error enum value, something is very wrong!!")
        }

    override fun allocationSize(value: Bech32Exception): Int = 4

    override fun write(
        value: Bech32Exception,
        buf: ByteBuffer,
    ) {
        when (value) {
            is Bech32Exception.Bech32EncodeException -> {
                buf.putInt(1)
                Unit
            }
            is Bech32Exception.Bech32DecodeException -> {
                buf.putInt(2)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class CryptoException(
    message: String,
) : Exception(message) {
    // Each variant is a nested class
    // Flat enums carries a string error message, so no special implementation is necessary.
    class Secp256k1Exception(
        message: String,
    ) : CryptoException(message)

    companion object ErrorHandler : CallStatusErrorHandler<CryptoException> {
        override fun lift(error_buf: RustBuffer.ByValue): CryptoException = FfiConverterTypeCryptoError.lift(error_buf)
    }
}

public object FfiConverterTypeCryptoError : FfiConverterRustBuffer<CryptoException> {
    override fun read(buf: ByteBuffer): CryptoException =
        when (buf.getInt()) {
            1 -> CryptoException.Secp256k1Exception(FfiConverterString.read(buf))
            else -> throw RuntimeException("invalid error enum value, something is very wrong!!")
        }

    override fun allocationSize(value: CryptoException): Int = 4

    override fun write(
        value: CryptoException,
        buf: ByteBuffer,
    ) {
        when (value) {
            is CryptoException.Secp256k1Exception -> {
                buf.putInt(1)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

public object FfiConverterSequenceUByte : FfiConverterRustBuffer<List<UByte>> {
    override fun read(buf: ByteBuffer): List<UByte> {
        val len = buf.getInt()
        return List<UByte>(len) {
            FfiConverterUByte.read(buf)
        }
    }

    override fun allocationSize(value: List<UByte>): Int {
        val sizeForLength = 4
        val sizeForItems = value.map { FfiConverterUByte.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(
        value: List<UByte>,
        buf: ByteBuffer,
    ) {
        buf.putInt(value.size)
        value.forEach {
            FfiConverterUByte.write(it, buf)
        }
    }
}

@Throws(CryptoException::class)
fun `signEcdsa`(
    `msg`: List<UByte>,
    `privateKeyBytes`: List<UByte>,
): List<UByte> =
    FfiConverterSequenceUByte.lift(
        rustCallWithError(CryptoException) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_sign_ecdsa(
                FfiConverterSequenceUByte.lower(`msg`),
                FfiConverterSequenceUByte.lower(`privateKeyBytes`),
                _status,
            )
        },
    )

@Throws(CryptoException::class)
fun `verifyEcdsa`(
    `msg`: List<UByte>,
    `signatureBytes`: List<UByte>,
    `publicKeyBytes`: List<UByte>,
): Boolean =
    FfiConverterBoolean.lift(
        rustCallWithError(CryptoException) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_verify_ecdsa(
                FfiConverterSequenceUByte.lower(`msg`),
                FfiConverterSequenceUByte.lower(`signatureBytes`),
                FfiConverterSequenceUByte.lower(`publicKeyBytes`),
                _status,
            )
        },
    )

@Throws(CryptoException::class)
fun `encryptEcies`(
    `msg`: List<UByte>,
    `publicKeyBytes`: List<UByte>,
): List<UByte> =
    FfiConverterSequenceUByte.lift(
        rustCallWithError(CryptoException) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_encrypt_ecies(
                FfiConverterSequenceUByte.lower(`msg`),
                FfiConverterSequenceUByte.lower(`publicKeyBytes`),
                _status,
            )
        },
    )

@Throws(CryptoException::class)
fun `decryptEcies`(
    `cipherText`: List<UByte>,
    `privateKeyBytes`: List<UByte>,
): List<UByte> =
    FfiConverterSequenceUByte.lift(
        rustCallWithError(CryptoException) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_decrypt_ecies(
                FfiConverterSequenceUByte.lower(`cipherText`),
                FfiConverterSequenceUByte.lower(`privateKeyBytes`),
                _status,
            )
        },
    )

@Throws(CryptoException::class)
fun `generateKeypair`(): KeyPair =
    FfiConverterTypeKeyPair.lift(
        rustCallWithError(CryptoException) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_generate_keypair(_status)
        },
    )

@Throws(Bech32Exception::class)
fun `encodeBech32`(
    `hrp`: String,
    `messageData`: List<UByte>,
): String =
    FfiConverterString.lift(
        rustCallWithError(Bech32Exception) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_encode_bech32(
                FfiConverterString.lower(`hrp`),
                FfiConverterSequenceUByte.lower(`messageData`),
                _status,
            )
        },
    )

@Throws(Bech32Exception::class)
fun `decodeBech32`(`bech32Str`: String): Bech32Data =
    FfiConverterTypeBech32Data.lift(
        rustCallWithError(Bech32Exception) { _status ->
            _UniFFILib.INSTANCE.uma_crypto_338f_decode_bech32(FfiConverterString.lower(`bech32Str`), _status)
        },
    )
