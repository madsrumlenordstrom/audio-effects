package utility

import chisel3._
import chisel3.util._

object Constants {
  val DATA_WIDTH = 32
  val BYTE_WIDTH = 8

  val CYCLONE_IV_FREQ = 50000000
  val I2C_FREQ = 12500000
  val WM8731_FREQ = -1

  val WM8731_I2C_ADDR = "b0011010"
}