package audio

import chisel3._
import utility.DSPModuleIO
import chisel3.util.RegEnable
import utility.Constants.CTRL_WIDTH

class DSPModule(defaultCtrl: Int = 0x0000) extends Module {
  val io = IO(new DSPModuleIO())
  // Input register
  val audioInReg = RegEnable(io.audioIn, io.clk)
  // Control register
  val ctrlReg = RegEnable(io.ctrlSig, defaultCtrl.U(CTRL_WIDTH.W), io.write)
}
