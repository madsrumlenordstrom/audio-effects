package io

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class WM8731ControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  "WM8731ControllerSpec" should "work" in {
    test(new WM8731Controller).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0) // Never make timeout
        dut.reset.poke(true.B)
        dut.clock.step(10)
        dut.reset.poke(false.B)
        dut.clock.step(100000)
    }
  }
}
