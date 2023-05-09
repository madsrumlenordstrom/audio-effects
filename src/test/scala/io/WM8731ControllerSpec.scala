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
        dut.clock.step(150000)

        // now after finishing the i2c configuration test the input
        dut.io.outData.poke(-0x111111.S)
        //dut.io.outData(1).poke(0x0bcdef.S)
        dut.clock.step(1) // sync outData inner register

        val adcSignal = Array(
            // channel 0
            0, // ignored
            0, 0, 0, 1, // 1
            0, 1, 1, 0, // 6
            0, 0, 1, 1, // 3
            0, 0, 1, 1, // 3
            1, 1, 1, 1, // f
            1, 1, 1, 1, // f
            // channel 1
            1, 0, 0, 1, // 9
            1, 1, 1, 0, // e
            1, 0, 1, 1, // b
            1, 0, 1, 1, // b
            0, 1, 1, 1, // 7
            0, 1, 1, 1, // 7
            0, 0, 0, 0, 0, // pad to 60
            0, 0, 0, 0, 0, 0,
        )

        // do twice
        for (j <- 0 until 3) {
            for (i <- 0 until 30 * 2 * 2) {
              // sync
              if (i  < 2) {
                dut.io.wm8731io.dac.daclrck.poke(true.B)
                dut.io.wm8731io.adc.adclrck.poke(true.B)
              } else {
                dut.io.wm8731io.dac.daclrck.poke(false.B)
                dut.io.wm8731io.adc.adclrck.poke(false.B)
              }

              // clock bclk
              if (i % 2 == 0) {
                dut.io.wm8731io.bclk.poke(false.B)
                dut.io.wm8731io.adc.adcdat.poke(adcSignal(i / 2).B)
              } else {
                dut.io.wm8731io.bclk.poke(true.B)
              }
              dut.clock.step(40)
            }
        }
    }
  }
}
