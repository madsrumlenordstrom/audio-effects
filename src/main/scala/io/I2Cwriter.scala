package io

import chisel3._
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.I2CwriterIO

object I2Cwriter {
  object State extends ChiselEnum {
    val idle, start0, start1, addr, regaddr, regdata, stop = Value
  }
}

class I2Cwriter extends Module {
  import I2Cwriter.State
  import I2Cwriter.State._

  val io = IO(new I2CwriterIO)

  // State machine register
  val state = WireDefault(idle)
  val stateReg = RegEnable(state, idle, io.enable)

  val counterEnable = WireDefault(false.B)
  val (counterValue, counterWrap) = Counter(io.enable && counterEnable, 8)

  // Default values
  io.ready := WireDefault(false.B)
  io.i2c.sclk := WireDefault(true.B)
  io.i2c.sdat := WireDefault(true.B)

  // State machine
  switch (stateReg) {
    is (idle) {
      io.ready := true.B
      when (io.write) {
        state := start0
      }
    }
    is (start0) {
      io.i2c.sdat := false.B
      state := start1
    }
    is (start1) {
      io.i2c.sdat := false.B
      io.i2c.sclk := false.B
      state := addr
    }
    is (addr) {
      counterEnable := true.B
      when (counterWrap) {
        state := regaddr
      }
    }
  }
}
