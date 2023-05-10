package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.FlatSpec

class OctaveDownTest extends AnyFlatSpec with ChiselScalatestTester {

  "OctaveUp" should "play" in {
    test(new OctaveDown()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)

      var finished = false
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      dut.io.clk.poke(true.B)
    //   dut.io.out.ready.poke(true.B)
      
      // Write the samples
      val th = fork {
        // dut.io.in.valid.poke(true.B)
        for (s <- samples) {
          dut.io.audioIn.poke(s.asSInt)
          dut.clock.step()
        //   while (!dut.io.in.ready.peek.litToBoolean) {
        //     dut.clock.step()
        //   }
        }
        finished = true
      }

      // Playing in real-time does not work, so record the result
      var idx = 0
      while (!finished) {
      // for (j <- 0 to 40) {
        // val valid = dut.io.out.valid.peek.litToBoolean
        // if (valid) {
        val s = dut.io.audioOut.peek.litValue.toShort
        outSamples(idx) = s
        idx += 1
        // }
        dut.clock.step()
      }
      th.join()

      // Uncomment for direct playback
      //startPlayer
      //playArray(outSamples)      
      //stopPlayer

      saveArray(outSamples, "sample_octavedown_out.wav")
    }
  }
}
