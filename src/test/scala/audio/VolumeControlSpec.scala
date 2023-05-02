package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import firrtl.Utils

class VolumeControlSpec extends AnyFlatSpec with ChiselScalatestTester {

  "VolumeControl" should "play" in {
    test(new VolumeControl()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)

      var finished = false
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      dut.io.clk.poke(true.B)
      // Set volume of module
      dut.io.ctrlSig.poke("b01000000".U)

      // Write the samples
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

      saveArray(outSamples, "sample_out.wav")
    }
  }
}

