package utility

import chisel3._
import chisel3.util.Counter

class ClockDivider(baseFreq: Int, outFreq: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clk = Output(Bool())
  })

  // Check arguments
  if (outFreq > baseFreq) {
    throw new IllegalArgumentException("freq cannot be lower than baseFreq")
  }

  // Use chisel counter
  val clk = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, baseFreq/outFreq)

  // Flip clock
  when(counterWrap) {
    clk := ~clk
  }

  io.clk := clk
}
