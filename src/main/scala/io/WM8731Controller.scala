package io

import chisel3._
import chisel3.util.{switch, is}
import chisel3.experimental.Analog

import utility.I2CIO
import utility.Constants._
import utility.ClockDividerByFreq

// IO bundle for ADC
class WM8731IO_ADC extends Bundle {
  val adclrck = Output(Bool())
  val adcdat = Input(Bool())
}

// IO bundle for DAC
class WM8731IO_DAC extends Bundle {
  val daclrck = Output(Bool())
  val dacdat = Output(Bool())
}

// Audio CODEC IO
class WM8731IO extends Bundle {
  val adc = new WM8731IO_ADC()
  val dac = new WM8731IO_DAC()
  val bclk = Output(Bool())         // bit-stream clock
  val xck = Output(Bool())          // chip clock
  val i2c = new I2CIO()
}

class WM8731ControllerIO extends Bundle {
  val ready = Output(Bool())            // the controller has completed the setup over i2c
  val error = Output(Bool())            // indicate some error has happened
  val errorCode = Output(UInt(16.W))    // return error code to be displayed
  val wm8731io = new WM8731IO           // board pins
}

object WM8731Controller {
  // Note: currently supports write only
  object State extends ChiselEnum {
    val inReset, writing1, waiting1, ready, error = Value
  }
}

class WM8731Controller extends Module {
  import WM8731Controller.State
  import WM8731Controller.State._
  val io = IO(new WM8731ControllerIO)

  // default values
  io.wm8731io.dac.dacdat := true.B
  io.wm8731io.adc.adclrck := true.B
  io.wm8731io.bclk := true.B
  io.wm8731io.i2c.sclk := true.B
  io.wm8731io.dac.daclrck := true.B

  val readyReg = RegInit(false.B)
  val errorReg = RegInit(false.B)
  val errorCodeReg = RegInit(0.U(16.W))
  io.ready := readyReg
  io.error := errorReg
  io.errorCode := errorCodeReg
  
  var stateReg = RegInit(inReset)

  // setup master clock
  val xckClockDivider = Module(new ClockDividerByFreq(CYCLONE_II_FREQ, WM8731_FREQ))
  io.wm8731io.xck := xckClockDivider.io.clk
  
  val i2cController = Module(new I2CController(WM8731_I2C_ADDR, WM8731_I2C_FREQ))
  i2cController.io.i2cio <> io.wm8731io.i2c
  // default values
  val i2cControllerStartReg = RegInit(false.B)
  val i2cControllerRegAddrReg = RegInit(0.U(8.W))
  val i2cControllerInReg = RegInit(0.U(8.W))
  i2cController.io.start := i2cControllerStartReg
  i2cController.io.regAddr := i2cControllerRegAddrReg
  i2cController.io.in := i2cControllerInReg
  
  switch (stateReg) {
    is (inReset) {
      stateReg := writing1
    }
    is (writing1) {
      i2cControllerInReg := "b00010000".U  // power most on
      i2cControllerRegAddrReg := "b0000110".U // power control reg
      i2cControllerStartReg := true.B
      stateReg := waiting1
    }
    is (waiting1) {
      // trigger start by negedge
      i2cControllerStartReg := false.B
      when (i2cController.io.error) {
        errorCodeReg := i2cController.io.errorCode
        stateReg := error
      }
      when (i2cController.io.done) {
        stateReg := ready
      }
    }
    is (ready) {
      readyReg := true.B
      // loop in this state
    }
    is (error) {
      errorReg := true.B
      // loop in this state
    }
  }
}
