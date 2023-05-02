package audio

import chisel3._

import utility.Constants.{DATA_WIDTH, CTRL_WIDTH}
import chisel3.util.log2Up

class AudioProcessingFrameControlIO(effects: Seq[DSPModule]) extends Bundle {
  val write = Input(Bool())
  val dspAddr = Input(UInt(log2Up(effects.length).W))
  val dspCtrl = Input(UInt(CTRL_WIDTH.W))
}

class AudioProcessingFrameIO(effects: Seq[DSPModule]) extends Bundle {
  // Audio input and output (L/R)
  val inData = Input(SInt(DATA_WIDTH.W))
  val outData = Output(SInt(DATA_WIDTH.W))
  val ctrl = new AudioProcessingFrameControlIO(effects)
}

class AudioProcessingFrame(effects: Seq[DSPModule]) extends Module {
  val io = IO(new AudioProcessingFrameIO(effects))

  // Registers for control values
  val ctrlRegs = Reg(Vec(effects.length, UInt(CTRL_WIDTH.W)))
  when (io.ctrl.write) {
    ctrlRegs(io.ctrl.dspAddr) := io.ctrl.dspCtrl
  }

  // Chain effects modules
  effects(0).io.audioIn := io.inData
  for (i <- 1 until effects.length) {
    effects(i).io.audioIn := effects(i - 1).io.audioOut
  }

  // Send control signal to modules
  for (i <- 0 until effects.length) {
    effects(i).io.ctrlSig := ctrlRegs(i)
  }
  
  io.outData := effects(effects.length - 1).io.audioOut
}