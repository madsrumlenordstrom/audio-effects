package audio

import audio.Sounds._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DistortionTest
    extends AnyFlatSpec
    with DSPModuleTestTools
    with ChiselScalatestTester {

  "Distortion" should "play" in {
    test(new Distortion()).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
      sendCtrlSig(dut, 10.U)
      simulateAudio(
        dut,
        outputFile = "sample-" + dut.desiredName.toLowerCase + ".wav"
      )
    }
  }
}
