package utility

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ClockDividerSpec extends AnyFlatSpec with ChiselScalatestTester {
  "ClockDivider" should "divide clock to closest possible value" in {
    test(new ClockDivider(100, 10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(100)
    }
  }
}