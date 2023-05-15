package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class NoiseGateSpec
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "NoiseGate" should "play" in {
    test(new NoiseGate(255, 16)).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
      simulateAudio(
        dut,
        inputFile = "sample.wav",
        outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
      )
    }
  }
}
