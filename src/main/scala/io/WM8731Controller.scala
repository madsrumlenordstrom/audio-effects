package io

import chisel3._
import chisel3.experimental.Analog

import utility.I2CIO

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

class WM8731Controller extends Module {
  val io = IO(new WM8731ControllerIO)

  // intialize outputs
  io.ready := false.B
  io.error := false.B
  io.errorCode := 0.U

  io.wm8731io.dac.dacdat := true.B
  io.wm8731io.adc.adclrck := true.B
  io.wm8731io.bclk := true.B
  io.wm8731io.xck := true.B
  io.wm8731io.i2c.sclk := true.B
  io.wm8731io.dac.daclrck := true.B
}
