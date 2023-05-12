package audio

import audio.Sounds._

import java.io.{File, ByteArrayInputStream}
import javax.sound.sampled.{
  AudioFormat,
  AudioSystem,
  AudioInputStream,
  AudioFileFormat
}

object Sounds {

  val buf = new Array[Byte](2)
  val af = new AudioFormat(44100.toFloat, 16, 1, true, true)

  def getFileSamples(s: String): Array[Short] = {
    var f = new File(s);
    var stream = AudioSystem.getAudioInputStream(f);
    stream = AudioSystem.getAudioInputStream(af, stream)

    var sampleCount = stream.getFrameLength().toInt
    var samples = new Array[Short](sampleCount)
    for (i <- 0 until sampleCount) {
      stream.read(buf, 0, 2)
      samples(i) = ((buf(0) << 8) | (buf(1) & 0xff)).toShort
    }

    return samples
  }

  def saveArray(arr: Array[Short], s: String) = {
    var f = new File(s)
    f.createNewFile()

    var arr_conv = new Array[Byte](arr.length * 2)
    for (i <- 0 until arr.length) {
      arr_conv(2 * i) = (arr(i) >> 8).toByte
      arr_conv(2 * i + 1) = (arr(i) & 0xff).toByte
    }

    var stream =
      new AudioInputStream(new ByteArrayInputStream(arr_conv), af, arr.length)
    AudioSystem.write(stream, AudioFileFormat.Type.WAVE, f)
  }
}
