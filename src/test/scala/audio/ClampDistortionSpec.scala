package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ClampDistortionSpec
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "ClampDistortion" should "play" in {
    test(new ClampDistortion(16, 4096 * 2))
      .withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
        dut =>
          simulateAudio(
            dut,
            outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
          )
      }
  }
}
