package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.FlatSpec

class DistortionTest extends AnyFlatSpec with ChiselScalatestTester {

  "Distortion" should "play" in {
    test(new Distortion()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)

      var finished = false
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      dut.io.clk.poke(true.B)
    	dut.io.ctrlSig.poke(10.asUInt)
      dut.io.write.poke(true.B)
      dut.clock.step(1)
      dut.io.write.poke(false.B)
      
      val th = fork {
        for (s <- samples) {
          dut.io.audioIn.poke(s.asSInt)
          dut.clock.step()
        }
        finished = true
      }

      // Playing in real-time does not work, so record the result
      var idx = 0
      while (!finished) {
        val s = dut.io.audioOut.peek().litValue.toShort
        outSamples(idx) = s
        idx += 1
        dut.clock.step()
      }
      th.join()

      // Uncomment for direct playback
      //startPlayer
      //playArray(outSamples)      
      //stopPlayer

      saveArray(outSamples, "sample_distortion_out.wav")
    }
  }
}
