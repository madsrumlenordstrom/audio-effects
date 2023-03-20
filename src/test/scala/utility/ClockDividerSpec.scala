package utility

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ClockDividerSpec extends AnyFlatSpec with ChiselScalatestTester {
  "ClockDividerByFreq" should "divide clock to closest possible value" in {
    test(new ClockDividerByFreq(100, 10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(100)
    }
  }
}

class I2CClockDividerByFreqSpec extends AnyFlatSpec with ChiselScalatestTester {
  "I2CClockDividerByFreq" should "divide clock to closest possible value" in {
    test(new I2CClockDividerByFreq(100, 10, 2)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(100)
    }
  }
}