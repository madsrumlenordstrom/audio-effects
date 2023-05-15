package audio

import chisel3._
import chisel3.util._

object NoiseGate {
  object State extends ChiselEnum {
    val open, closed = Value
  }
}

// Noise gate to reduce noise with no input
// Uses ctrl register for the threshold value
class NoiseGate(defaultCtrl: Int = 16, holdLength: Int = 16)
    extends DSPModule(defaultCtrl) {
  import NoiseGate.State
  import NoiseGate.State._

  // State machine register
  val state = RegInit(closed)

  // Register for holding noise gate open on zero crossings
  val holdRegs = Reg(Vec(holdLength, Bool()))

  // Holding condidtion
  holdRegs(0) := audioInReg.abs.asUInt < (ctrlReg >> 2).asUInt
  // Chain registers
  for (i <- 1 until holdRegs.length) {
    when(io.clk) {
      holdRegs(i) := holdRegs(i - 1)
    }
  }

  // AND registers together
  val closeCond = holdRegs.reduceTree(_ & _)

  switch(state) {
    is(open) {
      audioOut := audioInReg
      // Close gate
      when(closeCond) {
        state := closed
      }
    }
    is(closed) {
      audioOut := 0.S
      // Open gate on threshold
      when(audioInReg.abs.asUInt > ctrlReg) {
        state := open
      }
    }
  }
}
