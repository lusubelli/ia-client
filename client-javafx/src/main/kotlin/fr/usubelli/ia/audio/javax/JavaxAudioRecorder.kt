package fr.usubelli.ia.audio.javax

import fr.usubelli.ia.audio.AudioRecorder
import javafx.concurrent.Task
//import net.sourceforge.javaflacencoder.FLAC_FileEncoder
import java.io.*
import javax.sound.sampled.*
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioInputStream


class JavaxAudioRecorder : AudioRecorder {

    override fun record(time: Long): ByteArray {
        return FileInputStream(File("D:\\workspace\\ia-client\\client\\src\\main\\resources\\voice.flac")).readBytes()
    }

    /*
    // path of the wav file
    var wavFile = File("D:\\workspace\\ia-client\\client\\src\\main\\resources\\record.wav")

    // format of audio file
    var fileType: AudioFileFormat.Type = AudioFileFormat.Type.WAVE

    // the line from which audio data is captured
    var line: TargetDataLine? = null

    private fun getAudioFormat(): AudioFormat {
        val sampleRate = 48000f
        val sampleSizeInBits = 8
        val channels = 1
        val signed = true
        val bigEndian = true
        return AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian)
    }

    private var recording: Boolean = false

    override fun record(time: Long): ByteArray {

        val finishThread = Thread(object : Task<Void>() {
            override fun call(): Void? {
                Thread.sleep(time)
                finish()
                return null
            }
        })
        finishThread.isDaemon = true
        finishThread.start()

        val os = ByteArrayOutputStream()

        val format = getAudioFormat()
        val info = DataLine.Info(TargetDataLine::class.java, format)

        // checks if system supports the data line
        if (!AudioSystem.isLineSupported(info)) {
            println("Line not supported")
            System.exit(0)
        }
        line = AudioSystem.getLine(info) as TargetDataLine
        line!!.open(format)
        line!!.start()   // start capturing

        println("Start recording...")

        // start recording
        val bufferSize = (format.sampleRate * format.frameSize).toInt()
        val buffer = ByteArray(bufferSize)
        recording = true
        while (recording) {
            val count = line!!.read(buffer, 0, buffer.size)
            if (count > 0) {
                os.write(buffer, 0, count)
            }
        }
        os.close()
        val audioInputStream = AudioInputStream(
                ByteArrayInputStream(os.toByteArray()), format, bufferSize.toLong())

        val input = File("D:\\workspace\\ia-client\\client\\src\\main\\resources\\record.wav")
        val output = File("D:\\workspace\\ia-client\\client\\src\\main\\resources\\record.flac")
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, input)

        val flacEncoder = FLAC_FileEncoder()
        flacEncoder.encode(input, output)
        return FileInputStream(output).readBytes()
    }

    fun finish() {
        recording = false
        line!!.stop()
        line!!.close()
        println("Finished")
    }
    */

}

