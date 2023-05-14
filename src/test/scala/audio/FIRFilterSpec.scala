package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants.CTRL_WIDTH

class FIRFilterSpec
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "FIRFilter" should "play" in {
    def movingAverageSeq(n: Int): Seq[SInt] = {
      var tmp = Seq[SInt]()
      for (i <- 0 until n) {
        tmp = tmp :+ 1.S(CTRL_WIDTH.W)
      }
      println(tmp)
      return tmp.toSeq
    }

    test(
      new FIRFilter(
        Seq(
          8.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          0.S,
          8.S
        )
      )
    )
      .withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
        dut =>
          simulateAudio(
            dut,
            outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
          )
      }
  }
}
