package fr.usubelli.ia.audio

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.DataFormatException


interface AudioConverter {
}

class SimpleDecodeFlacToWav: AudioConverter {

    fun flacToWav(input: ByteArray): ByteArray {
        val bitInputStream = BitInputStream(BufferedInputStream(ByteArrayInputStream(input)))
        val byteArrayOutputStream = ByteArrayOutputStream()
        decodeFile(bitInputStream, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }


    fun decodeFile(inpu: BitInputStream, out: OutputStream) {
        // Handle FLAC header and metadata blocks
        if (inpu.readUint(32) != 0x664C6143)
            throw DataFormatException("Invalid magic string")
        var sampleRate = -1
        var numChannels = -1
        var sampleDepth = -1
        var numSamples: Long = -1
        var last = false
        while (!last) {
            last = inpu.readUint(1) != 0
            val type = inpu.readUint(7)
            val length = inpu.readUint(24)
            if (type == 0) {  // Stream info block
                inpu.readUint(16)
                inpu.readUint(16)
                inpu.readUint(24)
                inpu.readUint(24)
                sampleRate = inpu.readUint(20)
                numChannels = inpu.readUint(3) + 1
                sampleDepth = inpu.readUint(5) + 1
                numSamples = inpu.readUint(18).toLong() shl 18 or inpu.readUint(18).toLong()
                for (i in 0..15)
                    inpu.readUint(8)
            } else {
                for (i in 0 until length)
                    inpu.readUint(8)
            }
        }
        if (sampleRate == -1)
            throw DataFormatException("Stream info metadata block absent")
        if (sampleDepth % 8 != 0)
            throw RuntimeException("Sample depth not supported")

        // Start writing WAV file headers
        val sampleDataLen = numSamples * numChannels.toLong() * (sampleDepth / 8).toLong()
        writeString("RIFF", out)
        writeLittleInt(4, sampleDataLen.toInt() + 36, out)
        writeString("WAVE", out)
        writeString("fmt ", out)
        writeLittleInt(4, 16, out)
        writeLittleInt(2, 0x0001, out)
        writeLittleInt(2, numChannels, out)
        writeLittleInt(4, sampleRate, out)
        writeLittleInt(4, sampleRate * numChannels * (sampleDepth / 8), out)
        writeLittleInt(2, numChannels * (sampleDepth / 8), out)
        writeLittleInt(2, sampleDepth, out)
        writeString("data", out)
        writeLittleInt(4, sampleDataLen.toInt(), out)

        // Decode FLAC audio frames and write raw samples
        while (decodeFrame(inpu, numChannels, sampleDepth, out));
    }


    private fun writeLittleInt(numBytes: Int, `val`: Int, out: OutputStream) {
        for (i in 0 until numBytes)
            out.write(`val`.ushr(i * 8))
    }

    private fun writeString(s: String, out: OutputStream) {
        out.write(s.toByteArray(StandardCharsets.UTF_8))
    }


    private fun decodeFrame(`in`: BitInputStream, numChannels: Int, sampleDepth: Int, out: OutputStream): Boolean {
        // Read a ton of header fields, and ignore most of them
        var temp = `in`.readByte()
        if (temp == -1)
            return false
        val sync = temp shl 6 or `in`.readUint(6)
        if (sync != 0x3FFE)
            throw DataFormatException("Sync code expected")

        `in`.readUint(1)
        `in`.readUint(1)
        val blockSizeCode = `in`.readUint(4)
        val sampleRateCode = `in`.readUint(4)
        val chanAsgn = `in`.readUint(4)
        `in`.readUint(3)
        `in`.readUint(1)

        temp = Integer.numberOfLeadingZeros((`in`.readUint(8) shl 24).inv()) - 1
        for (i in 0 until temp)
            `in`.readUint(8)

        val blockSize: Int
        if (blockSizeCode == 1)
            blockSize = 192
        else if (2 <= blockSizeCode && blockSizeCode <= 5)
            blockSize = 576 shl blockSizeCode - 2
        else if (blockSizeCode == 6)
            blockSize = `in`.readUint(8) + 1
        else if (blockSizeCode == 7)
            blockSize = `in`.readUint(16) + 1
        else if (8 <= blockSizeCode && blockSizeCode <= 15)
            blockSize = 256 shl blockSizeCode - 8
        else
            throw DataFormatException("Reserved block size")

        if (sampleRateCode == 12)
            `in`.readUint(8)
        else if (sampleRateCode == 13 || sampleRateCode == 14)
            `in`.readUint(16)

        `in`.readUint(8)

        // Decode each channel's subframe, then skip footer
        val samples = Array(numChannels) { IntArray(blockSize) }
        decodeSubframes(`in`, sampleDepth, chanAsgn, samples)
        `in`.alignToByte()
        `in`.readUint(16)

        // Write the decoded samples
        for (i in 0 until blockSize) {
            for (j in 0 until numChannels) {
                var `val` = samples[j][i]
                if (sampleDepth == 8)
                    `val` += 128
                writeLittleInt(sampleDepth / 8, `val`, out)
            }
        }
        return true
    }


    private fun decodeSubframes(`in`: BitInputStream, sampleDepth: Int, chanAsgn: Int, result: Array<IntArray>) {
        val blockSize = result[0].size
        val subframes = Array(result.size) { LongArray(blockSize) }
        if (0 <= chanAsgn && chanAsgn <= 7) {
            for (ch in result.indices)
                decodeSubframe(`in`, sampleDepth, subframes[ch])
        } else if (8 <= chanAsgn && chanAsgn <= 10) {
            decodeSubframe(`in`, sampleDepth + if (chanAsgn == 9) 1 else 0, subframes[0])
            decodeSubframe(`in`, sampleDepth + if (chanAsgn == 9) 0 else 1, subframes[1])
            if (chanAsgn == 8) {
                for (i in 0 until blockSize)
                    subframes[1][i] = subframes[0][i] - subframes[1][i]
            } else if (chanAsgn == 9) {
                for (i in 0 until blockSize)
                    subframes[0][i] += subframes[1][i]
            } else if (chanAsgn == 10) {
                for (i in 0 until blockSize) {
                    val side = subframes[1][i]
                    val right = subframes[0][i] - (side shr 1)
                    subframes[1][i] = right
                    subframes[0][i] = right + side
                }
            }
        } else
            throw DataFormatException("Reserved channel assignment")
        for (ch in result.indices) {
            for (i in 0 until blockSize)
                result[ch][i] = subframes[ch][i].toInt()
        }
    }


    private fun decodeSubframe(`in`: BitInputStream, sampleDepth: Int, result: LongArray) {
        var sampleDepth = sampleDepth
        `in`.readUint(1)
        val type = `in`.readUint(6)
        var shift = `in`.readUint(1)
        if (shift == 1) {
            while (`in`.readUint(1) == 0)
                shift++
        }
        sampleDepth -= shift

        if (type == 0)
        // Constant coding
            Arrays.fill(result, 0, result.size, `in`.readSignedInt(sampleDepth).toLong())
        else if (type == 1) {  // Verbatim coding
            for (i in result.indices)
                result[i] = `in`.readSignedInt(sampleDepth).toLong()
        } else if (8 <= type && type <= 12)
            decodeFixedPredictionSubframe(`in`, type - 8, sampleDepth, result)
        else if (32 <= type && type <= 63)
            decodeLinearPredictiveCodingSubframe(`in`, type - 31, sampleDepth, result)
        else
            throw DataFormatException("Reserved subframe type")

        for (i in result.indices)
            result[i] = result[i] shl shift
    }


    private fun decodeFixedPredictionSubframe(`in`: BitInputStream, predOrder: Int, sampleDepth: Int, result: LongArray) {
        for (i in 0 until predOrder)
            result[i] = `in`.readSignedInt(sampleDepth).toLong()
        decodeResiduals(`in`, predOrder, result)
        restoreLinearPrediction(result, FIXED_PREDICTION_COEFFICIENTS[predOrder], 0)
    }

    private val FIXED_PREDICTION_COEFFICIENTS = arrayOf(intArrayOf(), intArrayOf(1), intArrayOf(2, -1), intArrayOf(3, -3, 1), intArrayOf(4, -6, 4, -1))


    private fun decodeLinearPredictiveCodingSubframe(`in`: BitInputStream, lpcOrder: Int, sampleDepth: Int, result: LongArray) {
        for (i in 0 until lpcOrder)
            result[i] = `in`.readSignedInt(sampleDepth).toLong()
        val precision = `in`.readUint(4) + 1
        val shift = `in`.readSignedInt(5)
        val coefs = IntArray(lpcOrder)
        for (i in coefs.indices)
            coefs[i] = `in`.readSignedInt(precision)
        decodeResiduals(`in`, lpcOrder, result)
        restoreLinearPrediction(result, coefs, shift)
    }


    private fun decodeResiduals(`in`: BitInputStream, warmup: Int, result: LongArray) {
        val method = `in`.readUint(2)
        if (method >= 2)
            throw DataFormatException("Reserved residual coding method")
        val paramBits = if (method == 0) 4 else 5
        val escapeParam = if (method == 0) 0xF else 0x1F

        val partitionOrder = `in`.readUint(4)
        val numPartitions = 1 shl partitionOrder
        if (result.size % numPartitions != 0)
            throw DataFormatException("Block size not divisible by number of Rice partitions")
        val partitionSize = result.size / numPartitions

        for (i in 0 until numPartitions) {
            val start = i * partitionSize + if (i == 0) warmup else 0
            val end = (i + 1) * partitionSize

            val param = `in`.readUint(paramBits)
            if (param < escapeParam) {
                for (j in start until end)
                    result[j] = `in`.readRiceSignedInt(param)
            } else {
                val numBits = `in`.readUint(5)
                for (j in start until end)
                    result[j] = `in`.readSignedInt(numBits).toLong()
            }
        }
    }


    private fun restoreLinearPrediction(result: LongArray, coefs: IntArray, shift: Int) {
        for (i in coefs.size until result.size) {
            var sum: Long = 0
            for (j in coefs.indices)
                sum += result[i - 1 - j] * coefs[j]
            result[i] += sum shr shift
        }
    }

}


class BitInputStream(val `in`: InputStream): AutoCloseable {

    private var bitBuffer: Long = 0
    private var bitBufferLen: Int = 0


    fun alignToByte() {
        bitBufferLen -= bitBufferLen % 8
    }


    fun readByte(): Int {
        return if (bitBufferLen >= 8)
            readUint(8)
        else
            `in`.read()
    }


    fun readUint(n: Int): Int {
        while (bitBufferLen < n) {
            val temp = `in`.read()
            if (temp == -1)
                throw EOFException()
            bitBuffer = bitBuffer shl 8 or temp.toLong()
            bitBufferLen += 8
        }
        bitBufferLen -= n
        var result = bitBuffer.ushr(bitBufferLen).toInt()
        if (n < 32)
            result = result and (1 shl n) - 1
        return result
    }


    fun readSignedInt(n: Int): Int {
        return readUint(n) shl 32 - n shr 32 - n
    }


    fun readRiceSignedInt(param: Int): Long {
        var `val`: Long = 0
        while (readUint(1) == 0)
            `val`++
        `val` = `val` shl param or readUint(param).toLong()
        return `val`.ushr(1) xor -(`val` and 1)
    }


    override fun close() {
        `in`.close()
    }

}

class SimpleEncodeWavToFlac {

    fun wavToFlac(input: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        encodeFile(ByteArrayInputStream(input), BitOutputStream(byteArrayOutputStream))
        return byteArrayOutputStream.toByteArray()
    }

fun encodeFile(`in`: InputStream, out: BitOutputStream) {
    // Read and parse WAV file headers
    if (readString(`in`, 4) != "RIFF")
        throw DataFormatException("Invalid RIFF file header")
    readLittleInt(`in`, 4)
    if (readString(`in`, 4) != "WAVE")
        throw DataFormatException("Invalid WAV file header")

    if (readString(`in`, 4) != "fmt ")
        throw DataFormatException("Unrecognized WAV file chunk")
    if (readLittleInt(`in`, 4) != 16)
        throw DataFormatException("Unsupported WAV file type")
    if (readLittleInt(`in`, 2) != 0x0001)
        throw DataFormatException("Unsupported WAV file codec")
    val numChannels = readLittleInt(`in`, 2)
    if (numChannels < 0 || numChannels > 8)
        throw RuntimeException("Too many (or few) audio channels")
    val sampleRate = readLittleInt(`in`, 4)
    if (sampleRate <= 0 || sampleRate >= 1 shl 20)
        throw RuntimeException("Sample rate too large or invalid")
    readLittleInt(`in`, 4)
    readLittleInt(`in`, 2)
    val sampleDepth = readLittleInt(`in`, 2)
    if (sampleDepth == 0 || sampleDepth > 32 || sampleDepth % 8 != 0)
        throw RuntimeException("Unsupported sample depth")

    if (readString(`in`, 4) != "data")
        throw DataFormatException("Unrecognized WAV file chunk")
    val sampleDataLen = readLittleInt(`in`, 4)
    if (sampleDataLen <= 0 || sampleDataLen % (numChannels * (sampleDepth / 8)) != 0)
        throw DataFormatException("Invalid length of audio sample data")

    // Start writing FLAC file header and stream info metadata block
    out.writeInt(32, 0x664C6143)
    out.writeInt(1, 1)
    out.writeInt(7, 0)
    out.writeInt(24, 34)
    out.writeInt(16, BLOCK_SIZE)
    out.writeInt(16, BLOCK_SIZE)
    out.writeInt(24, 0)
    out.writeInt(24, 0)
    out.writeInt(20, sampleRate)
    out.writeInt(3, numChannels - 1)
    out.writeInt(5, sampleDepth - 1)
    var numSamples = sampleDataLen / (numChannels * (sampleDepth / 8))
    out.writeInt(18, numSamples.ushr(18))
    out.writeInt(18, numSamples.ushr(0))
    for (i in 0..15)
        out.writeInt(8, 0)

    // Read raw samples and encode FLAC audio frames
    var i = 0
    while (numSamples > 0) {
        val blockSize = Math.min(numSamples, BLOCK_SIZE)
        encodeFrame(`in`, i, numChannels, sampleDepth, sampleRate, blockSize, out)
        numSamples -= blockSize
        i++
    }
}


private val BLOCK_SIZE = 4096


@Throws(IOException::class)
private fun readString(`in`: InputStream, len: Int): String {
    val temp = ByteArray(len)
    for (i in temp.indices) {
        val b = `in`.read()
        if (b == -1)
            throw EOFException()
        temp[i] = b.toByte()
    }
    return String(temp, StandardCharsets.UTF_8)
}


@Throws(IOException::class)
private fun readLittleInt(`in`: InputStream, n: Int): Int {
    var result = 0
    for (i in 0 until n) {
        val b = `in`.read()
        if (b == -1)
            throw EOFException()
        result = result or (b shl i * 8)
    }
    return result
}


@Throws(IOException::class)
private fun encodeFrame(`in`: InputStream, frameIndex: Int, numChannels: Int, sampleDepth: Int, sampleRate: Int, blockSize: Int, out: BitOutputStream) {
    val samples = Array(numChannels) { IntArray(blockSize) }
    val bytesPerSample = sampleDepth / 8
    for (i in 0 until blockSize) {
        for (ch in 0 until numChannels) {
            var `val` = 0
            for (j in 0 until bytesPerSample) {
                val b = `in`.read()
                if (b == -1)
                    throw EOFException()
                `val` = `val` or (b shl j * 8)
            }
            if (sampleDepth == 8)
                samples[ch][i] = `val` - 128
            else
                samples[ch][i] = `val` shl 32 - sampleDepth shr 32 - sampleDepth
        }
    }

    out.resetCrcs()
    out.writeInt(14, 0x3FFE)
    out.writeInt(1, 0)
    out.writeInt(1, 0)
    out.writeInt(4, 7)
    out.writeInt(4, if (sampleRate % 10 == 0) 14 else 13)
    out.writeInt(4, numChannels - 1)
    when (sampleDepth) {
        8 -> out.writeInt(3, 1)
        16 -> out.writeInt(3, 4)
        24 -> out.writeInt(3, 6)
        32 -> out.writeInt(3, 0)
        else -> throw IllegalArgumentException()
    }
    out.writeInt(1, 0)
    out.writeInt(8, 0xFC or frameIndex.ushr(30))
    var i = 24
    while (i >= 0) {
        out.writeInt(8, 0x80 or (frameIndex.ushr(i) and 0x3F))
        i -= 6
    }
    out.writeInt(16, blockSize - 1)
    out.writeInt(16, sampleRate / if (sampleRate % 10 == 0) 10 else 1)
    out.writeInt(8, out.crc8)

    for (chanSamples in samples)
        encodeSubframe(chanSamples, sampleDepth, out)
    out.alignToByte()
    out.writeInt(16, out.crc16)
}


private fun encodeSubframe(samples: IntArray, sampleDepth: Int, out: BitOutputStream) {
    out.writeInt(1, 0)
    out.writeInt(6, 1)  // Verbatim coding
    out.writeInt(1, 0)
    for (x in samples)
        out.writeInt(sampleDepth, x)
}

}
class BitOutputStream(val out: OutputStream) : AutoCloseable {

    private var bitBuffer: Long = 0
    private var bitBufferLen: Int = 0
    var crc8: Int = 0
    var crc16: Int = 0

    init {
        resetCrcs()
    }


    fun resetCrcs() {
        crc8 = 0
        crc16 = 0
    }


    fun alignToByte() {
        writeInt((64 - bitBufferLen) % 8, 0)
    }


    fun writeInt(n: Int, `val`: Int) {
        bitBuffer = bitBuffer shl n or (`val` and (1L shl n).toInt() - 1).toLong()
        bitBufferLen += n
        while (bitBufferLen >= 8) {
            bitBufferLen -= 8
            val b = bitBuffer.ushr(bitBufferLen).toInt() and 0xFF
            out.write(b)
            crc8 = crc8 xor b
            crc16 = crc16 xor (b shl 8)
            for (i in 0..7) {
                crc8 = crc8 shl 1 xor crc8.ushr(7) * 0x107
                crc16 = crc16 shl 1 xor crc16.ushr(15) * 0x18005
            }
        }
    }


    override fun close() {
        out.close()
    }
}