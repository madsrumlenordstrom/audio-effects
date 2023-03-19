package utility

import chisel3._

// I2C lines
class I2C extends Bundle {
  val sclk = Bool()
  val sdat = Bool()
}

// I2C master IO bundle
class I2CwriterIO extends Bundle {
  val i2c = Output(new I2C)
  val ready = Output(Bool())
  val write = Input(Bool())
  val enable = Input(Bool())
  val addr = Input(UInt(7.W))
  val regaddr = Input(UInt(7.W))
  val regdata = Input(UInt(9.W))
}

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
class WM8731IO_IO extends Bundle {
  val adc = new WM8731IO_ADC()
  val dac = new WM8731IO_DAC()
  val bclk = Output(Bool())
  val i2c = new I2CwriterIO()
}