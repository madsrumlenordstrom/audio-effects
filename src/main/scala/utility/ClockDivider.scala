package utility

import chisel3._
import chisel3.util.Counter
import chisel3.util.ShiftRegister

class ClockDividerByFreq(baseFreq: Int, outFreq: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clk = Output(Bool())
  })

  // Check arguments
  if (outFreq > baseFreq) {
    throw new IllegalArgumentException("freq cannot be lower than baseFreq")
  }

  // Use chisel counter
  val clk = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, baseFreq / outFreq / 2)

  // Flip clock
  when (counterWrap) {
    clk := ~clk
  }

  io.clk := clk
}

class ClockDivider(divideBy: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clk = Output(Clock())
  })

  // Check arguments
  if (divideBy % 2 == 1) {
    throw new IllegalArgumentException("divideBy must be even")
  }

  // Use chisel counter
  val clk = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, divideBy/2)

  // Flip clock
  when(counterWrap) {
    clk := ~clk
  }

  io.clk := clk
}

// Clock where 
class I2CClockDividerByFreq(baseFreq: Int, outFreq: Int, clkBDelay: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clkA = Output(Bool())
    val clkB = Output(Bool())
  })

  // Check arguments
  if (outFreq > baseFreq) {
    throw new IllegalArgumentException("freq cannot be lower than baseFreq")
  }

  // Use chisel counter
  val clkA = RegInit(startOn.B)
  val (counterValue, counterWrap) = Counter(true.B, baseFreq/outFreq)

  // Flip clock
  when(counterWrap) {
    clkA := ~clkA
  }

  io.clkA := clkA
  io.clkB := ShiftRegister(clkA, clkBDelay) && (counterValue < (baseFreq/outFreq - clkBDelay).U) && clkA
}
