package utility

import chisel3._
import chisel3.experimental.Analog
import utility.Constants.{DATA_WIDTH, CTRL_WIDTH}

// Bundle for the assigned pins
class I2CIO extends Bundle {
  val sclk = Analog(1.W)
  val sda = Analog(1.W)
}

// Tri-state driver bundle
class TriStateBusDriverIO(width: Int) extends Bundle {
  val in = Output(UInt(width.W)) // data on the bus
  val out = Input(UInt(width.W)) // data put on the bus if io.drive is asserted
  val bus = Analog(width.W) // the tri-state bus
  val drive = Input(Bool()) // when asserted the module drives the bus
}
