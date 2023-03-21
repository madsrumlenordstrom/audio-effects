package io

import chisel3._
import chiseltest._
import chiseltest.experimental.UncheckedClockPoke._
import org.scalatest.flatspec.AnyFlatSpec

class I2CControllerByteSpec extends AnyFlatSpec with ChiselScalatestTester {
  "I2CControllerByte" should "send byte on I2C bus" in {
    test(new I2CControllerByte).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      def stepClock() : Unit = {
        dut.io.clkA.poke(false.B)
        dut.clock.step(1)
        dut.io.clkA.poke(true.B)
        dut.clock.step(1)
      }
      stepClock()
      stepClock()
      stepClock()
      dut.io.byte.poke("b10101011".U)
      dut.io.start.poke(true.B)
      stepClock()
      dut.io.start.poke(false.B)
      for (i <- 0 until 16) {
        stepClock()
      }
      dut.io.byte.poke("b11010101".U)
      dut.io.start.poke(true.B)
      stepClock()
      dut.io.start.poke(false.B)
      for (i <- 0 until 16) {
        stepClock()
      }
    }
  }
}
