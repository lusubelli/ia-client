package fr.usubelli.ia.audio

// record duration, in milliseconds
const val RECORD_TIME: Long = 10000  // 5 secondes

interface AudioRecorder {

    fun record(time: Long = RECORD_TIME): ByteArray

}