package utility

import chisel3._
import chisel3.experimental.Analog

// Tri-state driver bundle
class TriStateDriverIO(width: Int) extends Bundle{
    val in = Output(UInt(width.W))       // data on the bus
    val out = Input(UInt(width.W))       // data put on the bus if io.drive is asserted
    val bus = Analog(width.W)            // the tri-state bus
    val drive = Input(Bool())            // when asserted the module drives the bus
}

// Bundle for the assigned pins
class I2CIO extends Bundle {
    val sclk = Output(Bool())
    val sda = Analog(1.W)
}

// I2C lines
class I2COutput extends Bundle {
  val sclk = Output(Bool())    // I2C clock
  val sda = new TriStateDriverIO(1)
  //val sdatOut = Output(Bool()) // Data to put on bus
  //val drive = Output(Bool())   // Drive bus
  //val sdatIn = Input(Bool())   // Data read from bus
}

//// I2C Controller byte IO bundle
class I2CControllerByteIO extends Bundle {
  val i2c = new I2COutput    // Bus IO
  val clkA = Input(Bool())   // Data clock
  val clkB = Input(Bool())   // Bus clock
  val start = Input(Bool())  // Start operation signal
  val read = Input(Bool())   // Read or write
  val byte = Input(UInt(8.W))// Data to write
  val done = Output(Bool())  // Operation done
}

// I2C Controller IO bundle
class I2CControllerIO extends Bundle {
  val i2c = new I2COutput
  val clkA = Input(Bool())
  val clkB = Input(Bool())
  val ready = Output(Bool())
  val start = Input(Bool())
  val byte = Input(UInt(8.W))
  val read = Input(Bool())
}
