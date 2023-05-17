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

class DSPModule(controlInit: Int = 0, bypassInit: Boolean = false)
    extends Module {
  val io = IO(new DSPModuleIO())

  def printConfig(): Unit = {
    println(this.desiredName + " configured as:")
    println("Control init: 0x" + controlInit.toHexString)
    println("Bypass  init: " + bypassInit)
    println()
  }

  // Input register
  val audioInReg = RegEnable(io.audioIn, 0.S, io.clk)

  // Output wire
  val audioOut = WireInit(0.S(DATA_WIDTH.W))

  // Control register
  val ctrlReg =
    RegEnable(io.ctrlSig, controlInit.U(CTRL_WIDTH.W), io.write && !io.bypass)
  io.strdCtrlSig := ctrlReg

  // Bypass register
  val bypassReg = RegEnable(io.bypass, bypassInit.B, io.write)
  io.strdBypass := bypassReg

  // Bypass module
  when(bypassReg) {
    io.audioOut := audioInReg
  }.otherwise {
    io.audioOut := audioOut
  }
}
