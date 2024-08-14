package me.uma.protocol

import me.uma.utils.TLVCodeable
import me.uma.utils.getBoolean
import me.uma.utils.getByteCodeable
import me.uma.utils.getNumber
import me.uma.utils.getString
import me.uma.utils.getTLV
import me.uma.utils.lengthOffset
import me.uma.utils.putByteCodeable
import me.uma.utils.putBoolean
import me.uma.utils.putByteArray
import me.uma.utils.putTLVCodeable
import me.uma.utils.putNumber
import me.uma.utils.putString
import me.uma.utils.valueOffset
import java.nio.ByteBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = InvoiceCurrencyTLVSerializer::class)
data class InvoiceCurrency(
    val code: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
) : TLVCodeable {

    companion object {
        fun fromTLV(bytes: ByteArray): InvoiceCurrency {
            var code = ""
            var name = ""
            var symbol = ""
            var decimals = -1
            var offset = 0
            while(offset < bytes.size) {
                val length = bytes[offset.lengthOffset()].toInt()
                when(bytes[offset].toInt()) {
                    0 -> code = bytes.getString(offset.valueOffset(), length)
                    1 -> name = bytes.getString(offset.valueOffset(), length)
                    2 -> symbol = bytes.getString(offset.valueOffset(), length)
                    3 -> decimals = bytes.getNumber(offset.valueOffset())
                }
                offset = offset.valueOffset() + length
            }
            return InvoiceCurrency(name, symbol, code, decimals)
        }
    }

    override fun toTLV(): ByteArray {
        val bytes = ByteBuffer.allocate(
            2 + name.length +
            2 + code.length +
            2 + symbol.length +
            3 // for int
        )
            .putString(0, code)
            .putString(1, name)
            .putString(2, symbol)
            .putNumber(3, decimals)
            .array()
        return bytes
    }
}

@OptIn(ExperimentalSerializationApi::class)
class InvoiceCurrencyTLVSerializer: KSerializer<InvoiceCurrency> {
    private val delegateSerializer = ByteArraySerializer()
    override val descriptor = SerialDescriptor("InvoiceCurrency", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: InvoiceCurrency) {
        encoder.encodeSerializableValue(
            delegateSerializer,
            value.toTLV()
        )
    }

    override fun deserialize(decoder: Decoder) = InvoiceCurrency.fromTLV(
        decoder.decodeSerializableValue(delegateSerializer)
    )
}

@OptIn(ExperimentalSerializationApi::class)
class InvoiceTLVSerializer: KSerializer<Invoice> {
    private val delegateSerializer = ByteArraySerializer()
    override val descriptor = SerialDescriptor("Invoice", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Invoice) {
        encoder.encodeSerializableValue(
            delegateSerializer,
            value.toTLV()
        )
    }

    override fun deserialize(decoder: Decoder) = Invoice.fromTLV(
        decoder.decodeSerializableValue(delegateSerializer)
    )
}

@Serializable(with = InvoiceTLVSerializer::class)
class Invoice(
    val receiverUma: String,

    // Invoice UUID Served as both the identifier of the UMA invoice, and the validation of proof of payment.
    val invoiceUUID: String,

    // The amount of invoice to be paid in the smalest unit of the ReceivingCurrency.
    val amount: Int,

    // The currency of the invoice
    val receivingCurrency: InvoiceCurrency,

    // The unix timestamp the UMA invoice expires
    val expiration: Int,

    // Indicates whether the VASP is a financial institution that requires travel rule information.
    val isSubjectToTravelRule: Boolean,

    // RequiredPayerData the data about the payer that the sending VASP must provide in order to send a payment.
    val requiredPayerData: CounterPartyDataOptions,

    // UmaVersion is a list of UMA versions that the VASP supports for this transaction. It should be
    // containing the lowest minor version of each major version it supported, separated by commas.
    val umaVersion: String,

    // CommentCharsAllowed is the number of characters that the sender can include in the comment field of the pay request.
    val commentCharsAllowed: Int,

    // The sender's UMA address. If this field presents, the UMA invoice should directly go to the sending VASP instead of showing in other formats.
    val senderUma: String,

    // The maximum number of the invoice can be paid
    val invoiceLimit: Int,

    // KYC status of the receiver, default is verified.
    val kycStatus: KycStatus,

    // The callback url that the sender should send the PayRequest to.
    val callback: String,

    // The signature of the UMA invoice
    val signature: ByteArray,
) : TLVCodeable {
    override fun toTLV(): ByteArray {
        val bytes = ByteBuffer.allocate(
            1 // TODO, this is too manual
        )
            .putString(0, receiverUma)
            .putString(1, invoiceUUID)
            .putNumber(2, amount)
            .putTLVCodeable(3, receivingCurrency)
            .putNumber(4, expiration)
            .putBoolean(5, isSubjectToTravelRule)
            .putByteCodeable(6, CounterPartyDataOptionsWrapper(requiredPayerData))
            .putString(7, umaVersion)
            .putNumber(8, commentCharsAllowed)
            .putString(9, senderUma)
            .putNumber(10, invoiceLimit)
            .putByteCodeable(11, KycStatusWrapper(kycStatus))
            .putString(12, callback)
            .putByteArray(100, signature)
            .array()
        return bytes
    }

    companion object {
        fun fromTLV(bytes: ByteArray): Invoice {
            var receiverUma = ""
            var invoiceUUID = ""
            var amount = -1
            var receivingCurrency: InvoiceCurrency
            var expiration = -1
            var isSubjectToTravelRule = false
            var requiredPayerData: CounterPartyDataOptions
            var umaVersion = ""
            var commentCharsAllowed = -1
            var senderUma = ""
            var invoiceLimit = -1
            var kycStatus: KycStatus
            var callback = ""
            var signature = ByteArray(0)
            var offset = 0
            while(offset < bytes.size) {
                val length = bytes[offset.lengthOffset()].toInt()
                when(bytes[offset].toInt()) {
                    0 -> receiverUma = bytes.getString(offset.valueOffset(), length)
                    1 -> invoiceUUID = bytes.getString(offset.valueOffset(), length)
                    2 -> amount = bytes.getNumber(offset.valueOffset())
                    3 -> receivingCurrency = bytes.getTLV(offset.valueOffset(), length, InvoiceCurrency::fromTLV) as InvoiceCurrency
                    4 -> expiration = bytes.getNumber(offset.valueOffset())
                    5 -> isSubjectToTravelRule = bytes.getBoolean(offset.valueOffset())
                    6 -> requiredPayerData = (bytes.getByteCodeable(
                            offset.valueOffset(),
                            length,
                            CounterPartyDataOptionsWrapper::fromBytes) as CounterPartyDataOptionsWrapper
                        ).options
                    7 -> umaVersion = bytes.getString(offset.valueOffset(), length)
                    8 -> commentCharsAllowed = bytes.getNumber(offset.valueOffset())
                    9 -> senderUma = bytes.getString(offset.valueOffset(), length)
                    10 -> invoiceLimit = bytes.getNumber(offset.valueOffset())
                    11 -> kycStatus = (bytes.getByteCodeable(offset.valueOffset(), length, KycStatusWrapper::fromBytes) as KycStatusWrapper).status
                    12 -> callback = bytes.getString(offset.valueOffset(), length)
                    100 -> signature = bytes.sliceArray(offset.valueOffset()..< offset.valueOffset()+length
                    )
                }
                offset = offset.valueOffset() + length
            }
            return Invoice(
                receiverUma,
                invoiceUUID,
                amount,
                receivingCurrency,
                expiration,
                isSubjectToTravelRule,
                requiredPayerData,
                umaVersion,
                commentCharsAllowed,
                senderUma,
                invoiceLimit,
                kycStatus,
                callback,
                signature,
            )
        }
    }
}
