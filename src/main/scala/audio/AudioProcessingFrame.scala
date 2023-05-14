package audio

import chisel3._

import utility.Constants.{DATA_WIDTH, CTRL_WIDTH}
import chisel3.util.log2Up
import chisel3.util.RegEnable

class AudioProcessingFrameControlIO extends Bundle {
  val write = Input(Bool())
  val dspAddr = Input(UInt(log2Up(DSPModules.effects.length).W))
  val dspCtrl = Input(UInt(CTRL_WIDTH.W))
  val dspBypass = Input(Bool())
  val strdCtrl = Output(UInt(CTRL_WIDTH.W))
  val strdBypass = Output(Bool())
}

class AudioProcessingFrameIO extends AudioProcessingFrameControlIO {
  // Audio input and output (L/R)
  val inData = Input(SInt(DATA_WIDTH.W))
  val outData = Output(SInt(DATA_WIDTH.W))
  val clk = Input(Bool())
}

class AudioProcessingFrame extends Module {
  val io = IO(new AudioProcessingFrameIO())

  // Initialize modules
  val effects =
    (0 until DSPModules.effects.length).map(i => DSPModules.effects(i))

  // Print configuration
  println("\nAudio chain is configured as:")
  effects.foreach(effect => print("-> " + effect.desiredName + " "))
  println()

  // Send signals to modules
  val write = Wire(Vec(effects.length, Bool()))
  val strdCtrl = Wire(Vec(effects.length, UInt(CTRL_WIDTH.W)))
  val strdBypass = Wire(Vec(effects.length, Bool()))
  for (i <- 0 until effects.length) {
    write(i) := WireDefault(false.B)
    strdCtrl(i) := effects(i).io.strdCtrlSig
    strdBypass(i) := effects(i).io.strdBypass
    effects(i).io.ctrlSig := io.dspCtrl
    effects(i).io.bypass := io.dspBypass
    effects(i).io.clk := io.clk
    effects(i).io.write := write(i)
  }

  // Send write signal to modules
  write(io.dspAddr) := io.write

  // Get current stored control signal
  io.strdCtrl := strdCtrl(io.dspAddr)

  // Get current store bypass signal
  io.strdBypass := strdBypass(io.dspAddr)

  // Chain effects modules
  effects(0).io.audioIn := io.inData
  for (i <- 1 until effects.length) {
    effects(i).io.audioIn := effects(i - 1).io.audioOut
  }

  io.outData := effects(effects.length - 1).io.audioOut
}
