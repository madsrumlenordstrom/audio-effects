package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class OctaveDownTest
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "OctaveDown" should "play" in {
    test(new OctaveDown()).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
      simulateAudio(
        dut,
        outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
      )
    }
  }
}
