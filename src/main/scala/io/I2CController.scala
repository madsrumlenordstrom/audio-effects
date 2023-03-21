package io

import chisel3._
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.{I2CControllerIO, I2CControllerByteIO}
import utility.Constants.BYTE_WIDTH

// State machine for sending/receiving byte via I2C
class I2CControllerByte extends Module {
  val io = IO(new I2CControllerByteIO)

  // Default values
  io.i2c.sclk := io.clkB
  io.i2c.drive := WireDefault(true.B)
  io.done := WireDefault(false.B)

  // Counter to serialize byte
  val cntrEnable = WireDefault(true.B)
  val (cntrValue, _) = Counter(cntrEnable && io.clkA, BYTE_WIDTH)

  // Counter increment logic
  when (cntrValue === 0.U) {
    cntrEnable := io.start
  }

  // Signal when done
  when (cntrValue === (BYTE_WIDTH - 1).U) {
    io.done := true.B
  }

  // Output data
  val idx = (BYTE_WIDTH - 1).U - cntrValue
  io.i2c.sdatOut := io.byte(idx)
}

// Design roughly follow this description:
// https://www.ti.com/lit/an/slva704/slva704.pdf

object I2CController {
  object State extends ChiselEnum {
    val idle, startSdat, startSclk, writeByte, ack, regaddr, regdata, stop = Value
  }
}

// State machine for controlling an I2C bus
class I2CController extends Module {
  import I2CController.State
  import I2CController.State._

  val io = IO(new I2CControllerIO)

  // Default values
  io.ready := WireDefault(false.B)
  io.i2c.sclk := WireDefault(true.B)
  io.i2c.sdatOut := WireDefault(true.B)
  io.i2c.drive := WireDefault(true.B)

  // Sample condition
  val sample = io.ready && io.start && io.clkA

  // Sample register
  val byteReg = RegEnable(io.byte, sample)

  // Sample read value on start
  val readReg = RegEnable(io.read, sample)

  // State machine register
  val state = WireDefault(idle)
  val stateReg = RegEnable(state, idle, io.clkA)
  state := stateReg

  // Byte controller
  val byteCtrl = new I2CControllerByte
  byteCtrl.io.byte := io.byte
  byteCtrl.io.start := WireDefault(false.B)
  byteCtrl.io.clkA := io.clkA
  byteCtrl.io.clkA := io.clkB

  // State machine
  switch (stateReg) {
    is (idle) {
      io.ready := true.B
      when (io.start) {
        state := startSdat
      }
    }
    is (startSdat) {
      io.i2c.sdatOut := false.B
      state := startSclk
    }
    is (startSclk) {
      io.i2c.sclk := false.B
      io.i2c.sdatOut := false.B
      state := writeByte
    }
    is (writeByte) {
      byteCtrl.io.start := true.B
      io.i2c <> byteCtrl.io.i2c // Let bytecontroller take control of bus
      when (byteCtrl.io.done) {
        state := ack
      }
    }
    is (ack) {

    }
  }
}