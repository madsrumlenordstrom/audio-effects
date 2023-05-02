package audio

import chisel3._
import utility.DSPModuleIO
import chisel3.util.RegEnable

class DSPModule extends Module {
  val io = IO(new DSPModuleIO())
  // Input register
  val audioInReg = RegEnable(io.audioIn, io.clk)
}
