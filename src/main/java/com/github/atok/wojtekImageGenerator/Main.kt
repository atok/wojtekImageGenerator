package com.github.atok.wojtekImageGenerator
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.collections4.queue.CircularFifoQueue
import spark.Request
import spark.Spark.get
import spark.Spark.port
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO

val logBufferSize = 100

val font = Font(Font.MONOSPACED, Font.BOLD, 13)

val options = Options()
        .addOption("p", "port", true, "Http port of the server")

val imageWidth = 600
val imageHeight = 250

fun main(args: Array<String>) {
    val cmd = DefaultParser().parse(options, args)
    val port = cmd.getOptionValue("p", "80").toInt()

    port(port)

    val log = CircularFifoQueue<Map<String, String>>(logBufferSize)

    get("/image.png") { req, resp ->
        resp.header("Content-Type", "image/png")
        val data = exportRequestData(req)
        log.add(data)
        val imageText = data.map { "${it.key}: \n${it.value}" }.joinToString("\n\n")
        generateImage(imageText, imageWidth, imageHeight)
    }

    get("/log") { req, resp ->
        log.map { logItemToHtml(it) }.joinToString("\n<br/><br/>\n")
    }
}

fun drawText(g: Graphics2D, text: String, font: Font, x: Int, y: Int) {
    val fontMetrics = g.fontMetrics
    text.lines().forEachIndexed { i, line ->
        drawLine(g, line, font, x, y + i * fontMetrics.ascent)
    }
}

fun drawLine(g: Graphics2D, text: String, font: Font, x: Int, y: Int) {
    val fontMetrics = g.fontMetrics
    val textHeight = fontMetrics.ascent

    g.drawString(text, x, textHeight + y)
}

fun generateImage(text: String, width: Int, height: Int): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHints(RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON))
    graphics.font = font

    graphics.background = Color(0f, 0f, 0f, 0f)
    graphics.clearRect(0, 0, width, height)

    graphics.paint = Color.black

    drawText(graphics, text, font , 10, 0)

    val os = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", os)
    return os.toByteArray()
}

fun exportRequestData(req: Request): Map<String, String> {
    val headers = req.headers().map { " $it: ${req.headers(it)}"  }.joinToString("\n")
    val time = Instant.now()
    val ip = req.ip()
    return mapOf(
            "Time" to time.toString(),
            "IP" to ip,
            "Headers" to headers
    )
}

fun logItemToHtml(item: Map<String, String>): String {
    return item.entries.map {
        "<b>${it.key}: </b></br><code>${it.value.replace("\n", "<br/>")}</code>"
    }.joinToString("<br/>")
}