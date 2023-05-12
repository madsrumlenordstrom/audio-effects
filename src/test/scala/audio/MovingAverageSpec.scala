package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MovingAverageSpec
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "MovingAverage" should "play" in {
    test(new MovingAverage()).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
      simulateAudio(
        dut,
        outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
      )
    }
  }
}
