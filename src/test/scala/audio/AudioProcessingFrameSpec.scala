package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import firrtl.Utils// Specify which effects to use

class AudioProcessingFrameSpec extends AnyFlatSpec with ChiselScalatestTester {

  "AudioProcessingFrame" should "play" in {
    test(new AudioProcessingFrame()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      // Function to write to a DSP module
      def sendCtrlSig(addr: UInt, ctrl: UInt):Unit={
        dut.io.dspAddr.poke(addr)
        dut.io.dspCtrl.poke(ctrl)
        dut.io.write.poke(true.B)
        dut.clock.step()
        dut.io.dspAddr.poke(0.U)
        dut.io.dspCtrl.poke(0.U)
        dut.io.write.poke(false.B)
      }

      // Get audio file
      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)

      var finished = false
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)

      // Default values
      dut.io.clk.poke(true.B)
      dut.io.write.poke(false.B)

      // Write the samples
      val th = fork {
        for (s <- samples) {
          dut.io.inData.poke(s.asSInt)
          dut.clock.step()
        }
        finished = true
      }

      // Playing in real-time does not work, so record the result
      var idx = 0
      while (!finished) {
        val s = dut.io.outData.peek().litValue.toShort
        outSamples(idx) = s
        idx += 1
        dut.clock.step()
      }
      th.join()

      saveArray(outSamples, "sample_out.wav")
    }
  }
}

