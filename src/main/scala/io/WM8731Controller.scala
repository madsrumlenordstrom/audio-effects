package io

import chisel3._
import chisel3.util.{switch, is}
import chisel3.experimental.Analog

import utility.I2CIO
import utility.Constants._
import utility.AudioPLLDriver
import utility.AudioPLL

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
  val clock50 = Input(Bool())
  val ready = Output(Bool())            // the controller has completed the setup over i2c
  val error = Output(Bool())            // indicate some error has happened
  val errorCode = Output(UInt(16.W))    // return error code to be displayed
  val wm8731io = new WM8731IO           // board pins

  val inData = Vec(2, Output(SInt(24.W)))
  val outData = Vec(2, Input(SInt(24.W)))
}

object WM8731Controller {
  // Note: currently supports write only
  object State extends ChiselEnum {
    val inReset, initRegs, resetDevice, outputsPowerDown, setLeftLineIn, setRightLineIn, setFormat, setSampling, setAnalogPathControl, setDigitalPathControl, setActivate, powerOn, waitI2C, ready, error = Value
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
  val audioPLL = Module(new AudioPLLDriver())
  audioPLL.io.clock := io.clock50.asClock
  audioPLL.io.reset := this.reset
  io.wm8731io.xck := audioPLL.io.c0

  // make tests happy, isn't included in quartus project file
  // actually it instantiates a useless pll on the board, we
  // need to find out how to tell chisel it should compile it
  // even though instantiation is via inlined verilog in AudioPLLDriver..
  val dummyAudioPLL = Module(new AudioPLL)
  dummyAudioPLL.io.inclk0 := io.clock50.asClock
  dummyAudioPLL.io.areset := this.reset

  val i2sIn = Module(new I2S(0, 24))
  i2sIn.io.bclk := io.wm8731io.bclk
  i2sIn.io.lrc := io.wm8731io.adc.adclrck
  i2sIn.io.dat := io.wm8731io.adc.adcdat
  io.inData(0) := i2sIn.io.data(0).asSInt
  io.inData(1) := i2sIn.io.data(1).asSInt

  val i2sOut = Module(new I2S(1, 24))
  i2sOut.io.bclk := io.wm8731io.bclk
  i2sOut.io.lrc := io.wm8731io.dac.daclrck
  io.wm8731io.dac.dacdat := i2sOut.io.dat
  i2sOut.io.data(0) := io.outData(0).asUInt
  i2sOut.io.data(1) := io.outData(1).asUInt
  
  val i2cCtrl = Module(new I2CController(WM8731_I2C_ADDR, WM8731_I2C_FREQ))
  i2cCtrl.io.i2cio <> io.wm8731io.i2c
  // default values
  val i2cCtrlStartReg = RegInit(false.B)
  val i2cCtrlRegAddrReg = RegInit(0.U(7.W))
  val i2cCtrlInDataReg = RegInit(0.U(9.W))
  i2cCtrl.io.start := i2cCtrlStartReg
  i2cCtrl.io.regAddr := i2cCtrlRegAddrReg
  i2cCtrl.io.inData := i2cCtrlInDataReg
  
  switch (stateReg) {
    is (inReset) {
      stateReg := resetDevice
    }
    is (resetDevice) {
      // TODO: doesn't seem to actually make any difference...
      i2cCtrlRegAddrReg := "b0001111".U // reset register
      i2cCtrlInDataReg  := "b000000000".U // reset device
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := outputsPowerDown
    }
    is (outputsPowerDown) {
      i2cCtrlRegAddrReg := "b0000110".U // power register
      i2cCtrlInDataReg  := "b000010000".U // outputs power down
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setLeftLineIn
    }
    is (setLeftLineIn) {
      i2cCtrlRegAddrReg := "b0000000".U // left line in
      i2cCtrlInDataReg  := "b000010111".U // Vol=default, mute=0, load_both=0
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setRightLineIn
    }
    is (setRightLineIn) {
      i2cCtrlRegAddrReg := "b0000001".U // right line in
      i2cCtrlInDataReg  := "b000010111".U // Vol=default, mute=0, load_both=0
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
     nextStateAfterI2C := setFormat
    }
    is (setFormat) {
      i2cCtrlRegAddrReg := "b0000111".U // digital audio interface format
      i2cCtrlInDataReg  := "b001001010".U // Data = I2S, Bit length = 24 bits, master = on
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setSampling
    }
    is (setSampling) {
      i2cCtrlRegAddrReg := "b0001000".U // sampling control
      i2cCtrlInDataReg  := "b000000001".U // mode=usb, 250fs, sample rate - 48kHz
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setAnalogPathControl
    }
    is (setAnalogPathControl) {
      i2cCtrlRegAddrReg := "b0000100".U // analog audio path control
      i2cCtrlInDataReg  := "b000010010".U // DACSEL on, BYPASS off, INSEL=Line in, Mic mute to ADC
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setDigitalPathControl
    }
    is (setDigitalPathControl) {
      i2cCtrlRegAddrReg := "b0000101".U // digital audio path control
      i2cCtrlInDataReg  := "b000000000".U // DAC soft mute control off
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := setActivate
    }
    is (setActivate) {
      i2cCtrlRegAddrReg := "b0001001".U // active control
      i2cCtrlInDataReg  := "b000000001".U // active
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
      nextStateAfterI2C := powerOn
    }
    is (powerOn) {
      i2cCtrlRegAddrReg := "b0000110".U // power register
      i2cCtrlInDataReg  := "b000000000".U // all on
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
