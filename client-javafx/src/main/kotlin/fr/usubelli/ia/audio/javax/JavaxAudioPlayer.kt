package fr.usubelli.ia.audio.javax

import fr.usubelli.ia.audio.AudioPlayer
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream


class JavaxAudioPlayer: AudioPlayer {

    override fun play(sample: ByteArray) {

        val file = File("D:\\workspace\\ia-client\\client\\src\\main\\resources\\answer.mp3")
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(sample)
        fileOutputStream.close()

        val media = Media("file:///D://workspace//ia-client//client//src//main//resources//answer.mp3")
        val mediaPlayer = MediaPlayer(media)
        mediaPlayer.play()

    }

}
