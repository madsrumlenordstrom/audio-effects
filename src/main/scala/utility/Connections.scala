package utility

import chisel3._
import chisel3.experimental.Analog

// Tri-state driver bundle
class TriStateDriverIO(width: Int) extends Bundle{
    val busData = Output(UInt(width.W))  // data on the bus
    val driveData = Input(UInt(width.W)) // data put on the bus if io.drive is asserted
    val bus = Analog(width.W)            // the tri-state bus
    val drive = Input(Bool())            // when asserted the module drives the bus
}

// I2C lines
class I2COutput extends Bundle {
  val sclk = Output(Bool())    // I2C clock
  val sdatOut = Output(Bool()) // Data to put on bus
  val drive = Output(Bool())   // Drive bus
  val sdatIn = Input(Bool())   // Data read from bus
}

// I2C Controller byte IO bundle
class I2CControllerByteIO extends Bundle {
  val i2c = new I2COutput
  val clkA = Input(Bool())
  val clkB = Input(Bool())
  val done = Output(Bool())
  val start = Input(Bool())
  val byte = Input(UInt(8.W))
}

// I2C Controller IO bundle
class I2CControllerIO extends Bundle {
  val i2c = new I2COutput
  val clkA = Input(Bool())
  val clkB = Input(Bool())
  val ready = Output(Bool())
  val start = Input(Bool())
  val byte = Input(UInt(8.W))
  //val read = Input(Bool())
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
  val i2c = new I2CControllerIO()
}