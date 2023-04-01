package io

import chisel3._
import chisel3.util.{switch, is}
import chisel3.experimental.Analog

import utility.I2CIO
import utility.Constants._
import utility.ClockDividerByFreq

// IO bundle for ADC
class WM8731IO_ADC extends Bundle {
  // wc8731 runs in master mode, so we get the clock from the device
  val adclrck = Input(Bool())
  val adcdat = Input(Bool())
}

// IO bundle for DAC
class WM8731IO_DAC extends Bundle {
  // wc8731 runs in master mode, so we get the clock from the device
  val daclrck = Input(Bool())
  val dacdat = Output(Bool())
}

// Audio CODEC IO
class WM8731IO extends Bundle {
  val adc = new WM8731IO_ADC()
  val dac = new WM8731IO_DAC()
  // wc8731 runs in master mode, so we get the clock from the device
  val bclk = Input(Bool())          // bit-stream clock
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
    val inReset, initRegs, resetDevice, outputsPowerDown, setFormat, setAnalogPathControl, setDigitalPathControl, setActivate, powerOn, waitI2C, ready, error = Value
  }
}

class WM8731Controller extends Module {
  import WM8731Controller.State
  import WM8731Controller.State._
  val io = IO(new WM8731ControllerIO)

  // default values
  io.wm8731io.dac.dacdat := WireDefault(true.B)

  val readyReg = RegInit(false.B)
  val errorReg = RegInit(false.B)
  val errorCodeReg = RegInit(0.U(16.W))
  io.ready := readyReg
  io.error := errorReg
  io.errorCode := errorCodeReg
  
  var stateReg = RegInit(inReset)
  var nextStateAfterI2C = RegInit(inReset)

  // setup master clock
  val xckClockDivider = Module(new ClockDividerByFreq(CYCLONE_II_FREQ, WM8731_FREQ))
  io.wm8731io.xck := xckClockDivider.io.clk
  
  val i2cCtrl = Module(new I2CController(WM8731_I2C_ADDR, WM8731_I2C_FREQ))
  i2cCtrl.io.i2cio <> io.wm8731io.i2c
  // default values
  val i2cCtrlStartReg = RegInit(false.B)
  val i2cCtrlRegAddrReg = RegInit(0.U(8.W))
  val i2cCtrlInDataReg = RegInit(0.U(8.W))
  i2cCtrl.io.start := i2cCtrlStartReg
  i2cCtrl.io.regAddr := i2cCtrlRegAddrReg
  i2cCtrl.io.inData := i2cCtrlInDataReg
  
  switch (stateReg) {
    is (inReset) {
      stateReg := resetDevice
    }
    is (resetDevice) {
      // TODO: doesn't seem to actually make any difference...
      i2cCtrlRegAddrReg := "b00001111".U // reset register
      i2cCtrlInDataReg  := "b00000000".U // reset device
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := outputsPowerDown
    }
    is (outputsPowerDown) {
      i2cCtrlRegAddrReg := "b00000110".U // power register
      i2cCtrlInDataReg  := "b00010000".U // outputs power down
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setFormat
    }
    is (setFormat) {
      i2cCtrlRegAddrReg := "b00000111".U // digital audio interface format
      i2cCtrlInDataReg  := "b01001010".U // Data = I2S, Bit length = 24 bits, master = on
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setAnalogPathControl
    }
    is (setAnalogPathControl) {
      i2cCtrlRegAddrReg := "b00000100".U // analog audio path control
      i2cCtrlInDataReg  := "b00011000".U // DACSEL on, BYPASS on
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setDigitalPathControl
    }
    is (setDigitalPathControl) {
      i2cCtrlRegAddrReg := "b00000101".U // digital audio path control
      i2cCtrlInDataReg  := "b00000000".U // DAC soft mute control off
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setActivate
    }
    is (setActivate) {
      i2cCtrlRegAddrReg := "b00001001".U // active control
      i2cCtrlInDataReg  := "b00000001".U // active
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := powerOn
    }
    is (powerOn) {
      i2cCtrlRegAddrReg := "b00000110".U // power register
      i2cCtrlInDataReg  := "b00000000".U // all on
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := ready
    }
    is (waitI2C) {
      // trigger start by negedge
      i2cCtrlStartReg := false.B
      when (i2cCtrl.io.error) {
        // distinguish between i2c errors in different states by encoding the next state..
        errorCodeReg := i2cCtrl.io.errorCode | (nextStateAfterI2C.asUInt << 4.U)
        stateReg := error
      }
      when (i2cCtrl.io.done) {
        stateReg := nextStateAfterI2C
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
