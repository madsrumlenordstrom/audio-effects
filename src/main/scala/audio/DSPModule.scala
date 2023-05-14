package audio

import chisel3._
import chisel3.util.RegEnable

import utility.Constants.{CTRL_WIDTH, DATA_WIDTH}

class DSPModuleIO extends Bundle {
  val audioIn = Input(SInt(DATA_WIDTH.W)) // Data input for DSP
  val ctrlSig = Input(UInt(CTRL_WIDTH.W)) // Control signal for DSP module
  val bypass = Input(Bool())
  val write = Input(Bool()) // Signal for writing to control register
  val clk = Input(Bool()) // Audio clock
  val audioOut = Output(SInt(DATA_WIDTH.W)) // Data output for DSP
  val strdCtrlSig = Output(
    UInt(CTRL_WIDTH.W)
  ) // Stored ontrol signal for DSP module
  val strdBypass = Output(Bool()) // Stored bypass value
}

class DSPModule(defaultCtrl: Int = 0x0000) extends Module {
  val io = IO(new DSPModuleIO())

  // Input register
  val audioInReg = RegEnable(io.audioIn, io.clk)

  // Output wire
  val audioOut = Wire(SInt(DATA_WIDTH.W))

  // Control register
  val ctrlReg =
    RegEnable(io.ctrlSig, defaultCtrl.U(CTRL_WIDTH.W), io.write && !io.bypass)
  io.strdCtrlSig := ctrlReg

  // Bypass register
  val bypassReg = RegEnable(io.bypass, false.B, io.write)
  io.strdBypass := bypassReg

  // Bypass module
  when(bypassReg) {
    io.audioOut := audioInReg
  }.otherwise {
    io.audioOut := audioOut
  }
}
