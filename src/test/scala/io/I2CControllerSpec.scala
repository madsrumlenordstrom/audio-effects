package io

import chisel3._
import chiseltest._
import chiseltest.experimental.UncheckedClockPoke._
import org.scalatest.flatspec.AnyFlatSpec

class I2CControllerByteSpec extends AnyFlatSpec with ChiselScalatestTester {
  "I2CControllerByte" should "send byte on I2C bus" in {
    test(new I2CControllerByte).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.io.byte.poke("b11110000".U)
      dut.io.start.poke(true.B)
      //dut.io.enable.poke(true.B)
      for (i <- 0 until 10) {
        dut.io.start.poke(false.B)
      }
    }
  }
}
