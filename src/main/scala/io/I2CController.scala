package io

import chisel3._
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.{I2CControllerIO, I2CControllerByteIO}

object I2CControllerByte {
  object State extends ChiselEnum {
    val idle, running = Value
  }
}

// State machine for sending/receiving byte via I2C
class I2CControllerByte extends Module {
  import I2CControllerByte.State
  import I2CControllerByte.State._
  
  val io = IO(new I2CControllerByteIO)

  // Output data reg
  val byteReg = Reg(UInt(8.W))

  // State machine register
  val state = WireDefault(idle)
  val stateReg = RegNext(state, idle)
  
  // Counter to serialize byte
  val cntrEnable = WireDefault(false.B)
  val (cntrValue, cntrWrap) = Counter(cntrEnable, 8)
  switch (stateReg) {
    is (idle) {
      when (io.start) {
        byteReg := io.byte
        state := running
      }
    }
    is (running) {
      cntrEnable := true.B
      when (cntrWrap) {
        state := idle
      }
    }
  }
}

// Design roughly follow this description:
// https://www.ti.com/lit/an/slva704/slva704.pdf

object I2CController {
  object State extends ChiselEnum {
    val idle, startSdat, startSclk, addr, regaddr, regdata, stop = Value
  }
}

// State machine for controlling an I2C bus
class I2CController extends Module {
  import I2CController.State
  import I2CController.State._

  val io = IO(new I2CControllerIO)

  val byter = new I2CControllerByte
  byter.io.clk := io.enable.asClock

  // Sample condition
  val sample = io.ready && io.start

  // Sample peripheral register
  val peripheralAddrReg = RegEnable(io.peripheralAddr ## 0.U(1.W), sample)

  // Sample read value on start
  //val readReg = RegEnable(io.read, sample)

  // Sample peripheral register address
  val regAddrReg = RegEnable(io.regAddr, sample)

  // Sample peripheral register data
  val regDataInReg = RegEnable(io.regDataIn, sample)

  // State machine register
  val state = WireDefault(idle)
  val stateReg = RegEnable(state, idle, io.enable)

  val cntrEnable = WireDefault(false.B)
  val (cntrValue, cntrWrap) = Counter(io.enable && cntrEnable, 8)

  // Default values
  io.ready := WireDefault(false.B)
  io.i2c.sclk := WireDefault(true.B)
  io.i2c.sdatOut := WireDefault(true.B)
  //io.i2c.drive := WireDefault(true.B)

  // State machine
  switch (stateReg) {
    is (idle) {
      io.ready := true.B
      when (io.start) {
        state := startSdat
      }
    }
    is (startSdat) {
      state := startSclk
    }
    is (startSclk) {
      io.i2c.sclk := false.B
      state := addr
    }
    is (addr) {
      cntrEnable := true.B
      when (cntrWrap) {
        state := regaddr
      }
    }
  }
}
