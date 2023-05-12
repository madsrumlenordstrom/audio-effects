package audio

import audio.Sounds._

import chisel3._
import chisel3.util.Cat
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import firrtl.Utils
import utility.Constants.CTRL_WIDTH

class VolumeControlSpec extends AnyFlatSpec with ChiselScalatestTester {

  "VolumeControl" should "play" in {
    test(new VolumeControl()).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
      // Function to write to the DSP module
      def sendCtrlSig(ctrl: UInt): Unit = {
        dut.io.ctrlSig.poke(ctrl)
        dut.io.write.poke(true.B)
        dut.clock.step()
        dut.io.ctrlSig.poke(0.U)
        dut.io.write.poke(false.B)
      }

      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)

      var finished = false

      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      dut.io.clk.poke(true.B)

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

      saveArray(outSamples, "sample_out.wav")
    }
  }
}
