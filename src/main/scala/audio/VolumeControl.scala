package audio

import chisel3._
import utility.Constants.CTRL_WIDTH

class VolumeControl extends DSPModule {
  // This module will have no bypass

  // Create a fixed pint version of audio signal
  val audioInFix = audioInReg ## 0.S((CTRL_WIDTH/2).W)

  // Multiply by control signal
  io.audioOut := (audioInFix*(0.U(1.W) ## io.ctrlSig).asSInt) >> CTRL_WIDTH
}
