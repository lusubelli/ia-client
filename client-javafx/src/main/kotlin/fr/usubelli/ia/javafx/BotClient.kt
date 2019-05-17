package fr.usubelli.ia.javafx

import com.github.sarxos.webcam.Webcam
import fr.usubelli.ia.Bot
import fr.usubelli.ia.audio.javax.JavaxAudioPlayer
import fr.usubelli.ia.audio.javax.JavaxAudioRecorder
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.stage.Stage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO


class BotClient : Application() {

    private val bot = Bot()
    private val audioRecorder = JavaxAudioRecorder()
    private val audioPlayer = JavaxAudioPlayer()
    private var webCam: Webcam? = null
    private val imageProperty = SimpleObjectProperty<Image>()
    private val recognitionProperty = SimpleObjectProperty<Image>()

    override fun start(primaryStage: Stage) {

        initializeWebCam()


        val webCamPane = BorderPane()
        webCamPane.style = "-fx-background-color: #ccc;"
        webCamPane.center = imagesPane()
        webCamPane.right = chatPane()
        webCamPane.bottom = controlPane()
        primaryStage.title = "Connecting Camera Device Using Webcam Capture API"
        primaryStage.scene = Scene(webCamPane)
        primaryStage.centerOnScreen()
        primaryStage.sizeToScene()
        primaryStage.show()
    }

    private fun imagesPane(): Pane {
        val grid = GridPane()
        grid.alignment = Pos.CENTER
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(25.0, 25.0, 25.0, 25.0)

        val recognitionImage = ImageView()
        recognitionImage.imageProperty().bind(recognitionProperty)
        recognitionImage.minHeight(700.toDouble())
        recognitionImage.minWidth(600.toDouble())
        //recognitionImage.fitHeight = 400.0
        //recognitionImage.fitWidth = 400.0
        recognitionImage.prefHeight(500.0)
        recognitionImage.prefWidth(400.0)
        recognitionImage.isPreserveRatio = true

        grid.add(recognitionImage, 1, 1)

        val webcamImage = ImageView()
        webcamImage.imageProperty().bind(imageProperty)
        webcamImage.minHeight(700.toDouble())
        webcamImage.minWidth(600.toDouble())
        //webcamImage.fitHeight = 400.0
        //webcamImage.fitWidth = 400.0
        webcamImage.prefHeight(500.0)
        webcamImage.prefWidth(400.0)
        webcamImage.isPreserveRatio = true

        grid.add(webcamImage, 1, 2)

        return grid
    }

    private fun chatPane(): Pane {

        val grid = GridPane()
        grid.alignment = Pos.CENTER
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(25.0, 25.0, 25.0, 25.0)

        val textArea = TextArea()
        grid.add(textArea, 1, 1)

        val textField = TextField()
        grid.add(textField, 1, 2)

        val btn = Button("Send")
        btn.setOnAction {
            bot.writeMuted(textField.text)
                    .subscribe({
                        textArea.appendText(it + "\n")
                    }, {
                        it.printStackTrace()
                    }, {
                        textField.text = ""
                    })
        }
        val hbBtn = HBox(10.0)
        hbBtn.alignment = Pos.BOTTOM_RIGHT
        hbBtn.children.add(btn)
        grid.add(hbBtn, 1, 3)

        return grid

    }

    private var recognizeActivated: Boolean = false

    private fun controlPane(): Node {
        val hbox = HBox()
        hbox.padding = Insets(15.toDouble(), 12.toDouble(), 15.toDouble(), 12.toDouble())
        hbox.spacing = 10.0
        hbox.style = "-fx-background-color: #336699;"

        val talkButton = Button("Talk")
        talkButton.setPrefSize(100.toDouble(), 20.toDouble())
        talkButton.setOnAction {
            recordAndPlay()
        }

        val recognize = ToggleButton("Recognize")
        recognize.setPrefSize(100.toDouble(), 20.toDouble())
        recognize.setOnAction {
            recognizeActivated = !recognizeActivated
        }
        hbox.children.addAll(talkButton)
        hbox.children.addAll(recognize)
        return hbox
    }

    private fun initializeWebCam() {

        val webCamTask = object : Task<Void>() {
            override fun call(): Void? {
                webCam = Webcam.getWebcams()[0]
                webCam!!.open()

                startWebCamStream()

                return null
            }
        }

        val webCamThread = Thread(webCamTask)
        webCamThread.isDaemon = true
        webCamThread.start()

    }

    private fun startWebCamStream() {

        val th = Thread(object : Task<Void>() {
            override fun call(): Void? {
                var img: BufferedImage?
                while (webCam!!.isOpen) {
                    try {
                        img = webCam!!.image
                        if ((img) != null) {
                            if (recognizeActivated) {
                                recognize(img)
                            } else {
                                val ref = AtomicReference<WritableImage>()
                                ref.set(SwingFXUtils.toFXImage(img, ref.get()))
                                img.flush()
                                Platform.runLater({ imageProperty.set(ref.get()) })
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return null
            }
        })
        th.isDaemon = true
        th.start()


    }

    private fun recordAndPlay() {
        val th = Thread(object : Task<Void>() {

            override fun call(): Void? {

                bot
                        .say(audioRecorder.record())
                        .subscribe({ data ->
                            audioPlayer.play(data)
                        }, { error ->
                            error.printStackTrace()
                        }, {
                            println("complete")
                        })

                return null
            }

        })
        th.isDaemon = true
        th.start()

    }

    private fun recognize(img: BufferedImage) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(img, "jpg", byteArrayOutputStream)
        bot.recognise(byteArrayOutputStream.toByteArray())
                .subscribe(
                        { data ->
                            val image =ImageIO.read(ByteArrayInputStream(data))
                            val ref = AtomicReference<WritableImage>()
                            ref.set(SwingFXUtils.toFXImage(image, ref.get()))
                            image.flush()
                            Platform.runLater({ imageProperty.set(ref.get()) })
                        },
                        { error ->
                            val ref = AtomicReference<WritableImage>()
                            ref.set(SwingFXUtils.toFXImage(img, ref.get()))
                            img.flush()
                            Platform.runLater({ imageProperty.set(ref.get()) })
                        },
                        {}
                )
    }

}

fun main(args: Array<String>) {
    Application.launch(BotClient::class.java)
}
