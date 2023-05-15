package io

import chisel3._
import chisel3.util.{switch, is}
import chisel3.experimental.Analog

import utility.I2CIO
import utility.Constants._
import utility.AudioPLLDriver
import utility.AudioPLL
import os.read

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
  val bclk = Input(Bool()) // bit-stream clock
  val xck = Output(Bool()) // chip clock
  val i2c = new I2CIO()
}

class WM8731ControllerIO extends Bundle {
  val clock50 = Input(Bool())
  val ready = Output(Bool()) // the controller has completed the setup over i2c
  val error = Output(Bool()) // indicate some error has happened
  val errorCode = Output(UInt(16.W)) // return error code to be displayed
  val wm8731io = new WM8731IO // board pins

  // Stereo
  val inData = Vec(2, Output(SInt(DATA_WIDTH.W)))
  val outData = Vec(2 ,Input(SInt(DATA_WIDTH.W)))
  val sync = Output(Bool()) // true when a frame is ready

  val bypass = Input(Bool())
}

object WM8731Controller {
  // Note: currently supports write only
  object State extends ChiselEnum {
    val inReset, writeRegister, waitI2C, ready, error = Value
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

  val i2sIn = Module(new I2S(0, DATA_WIDTH))
  i2sIn.io.bclk := io.wm8731io.bclk
  i2sIn.io.lrc := io.wm8731io.adc.adclrck
  i2sIn.io.dat := io.wm8731io.adc.adcdat

  io.inData := i2sIn.io.data
  io.sync := i2sIn.io.sync

  val i2sOut = Module(new I2S(1, DATA_WIDTH))
  i2sOut.io.bclk := io.wm8731io.bclk
  i2sOut.io.lrc := io.wm8731io.dac.daclrck
  when(io.bypass) {
    io.wm8731io.dac.dacdat := io.wm8731io.adc.adcdat
  }.otherwise {
    io.wm8731io.dac.dacdat := i2sOut.io.dat
  }
  i2sOut.io.data(0) := io.outData(0).asSInt
  i2sOut.io.data(1) := io.outData(1).asSInt

  val i2cCtrl = Module(new I2CController(WM8731_I2C_ADDR, WM8731_I2C_FREQ))
  i2cCtrl.io.i2cio <> io.wm8731io.i2c
  // default values
  val i2cCtrlStartReg = RegInit(false.B)
  val i2cCtrlRegAddrReg = RegInit(0.U(7.W))
  val i2cCtrlInDataReg = RegInit(0.U(9.W))
  i2cCtrl.io.start := i2cCtrlStartReg
  i2cCtrl.io.regAddr := i2cCtrlRegAddrReg
  i2cCtrl.io.inData := i2cCtrlInDataReg

  // list of (reg addr, reg value) to configure
  val registerVals = VecInit(
    VecInit("b0001111".U(7.W),      // reset register
       "b000000000".U(9.W)),        // reset device

    VecInit("b0000110".U(7.W),      // power register
       "b000010000".U(9.W)),        // outputs power down

    VecInit("b0000010".U(7.W),      // left line out
       "b001111001".U(9.W)),        // Vol=0db 

    VecInit("b0000011".U(7.W),      // right line out
       "b001111001".U(9.W)),        // Vol=0db 

    VecInit("b0000000".U(7.W),      // left line in
       "b000010111".U(9.W)),        // Vol=default, mute=0, load_both=0

    VecInit("b0000001".U(7.W),      // right line in
       "b000010111".U(9.W)),        // Vol=default, mute=0, load_both=0

    VecInit("b0000111".U(7.W),      // digital audio interface format
       "b001011011".U(9.W)),        // Data = dsp, msb on 2nd bit, Bit length = 24 bits, master = on

    VecInit("b0001000".U(7.W),      // sampling control
       "b000000001".U(9.W)),        // mode=usb, 250fs, sample rate - 48kHz

    VecInit("b0000100".U(7.W),      // analog audio path control
       "b000010010".U(9.W)),        // DACSEL on, BYPASS off, INSEL=Line in, Mic mute to ADC

    VecInit("b0000101".U(7.W),      // digital audio path control
       "b000000000".U(9.W)),        // DAC soft mute control off

    VecInit("b0001001".U(7.W),      // active control
       "b000000001".U(9.W)),        // active

    VecInit("b0000110".U(7.W),      // power register
       "b001100010".U(9.W)),        // disables unneeded functions
  )

  val registerNumReg = RegInit(0.U(7.W))

  switch (stateReg) {
    is (inReset) {
      stateReg := writeRegister
    }
    is (writeRegister) {
      i2cCtrlRegAddrReg := registerVals(registerNumReg)(0)
      i2cCtrlInDataReg := registerVals(registerNumReg)(1)
      i2cCtrlStartReg := true.B
      stateReg := waitI2C
    }
    is (waitI2C) {
      // trigger start by negedge
      i2cCtrlStartReg := false.B
      when(i2cCtrl.io.error) {
        // distinguish between i2c errors in different states by encoding the next state..
        errorCodeReg := i2cCtrl.io.errorCode | (registerNumReg << 4.U)
        stateReg := error
      }
      when(i2cCtrl.io.done) {
        when (registerNumReg === registerVals.length.U - 1.U) {
          stateReg := ready
        } .otherwise {
          registerNumReg := registerNumReg + 1.U
          stateReg := writeRegister
        }
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
