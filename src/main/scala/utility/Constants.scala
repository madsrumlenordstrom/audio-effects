package utility

import chisel3._
import chisel3.util._

object Constants {
  val DATA_WIDTH = 32
  val BYTE_WIDTH = 8

  val CYCLONE_IV_FREQ = 50000000
  val CYCLONE_II_FREQ = 50000000

  val WM8731_I2C_ADDR = 0x1a
  val WM8731_I2C_FREQ = 100000  // max is 526kHz
}
