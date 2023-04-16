package utility

import chisel3._
import chisel3.util.HasBlackBoxInline

class AudioPLLDriverIO extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val c0 = Output(Bool()) // 12MHz clock
}

// This module outputs 12MHz clock
class AudioPLLDriver() extends BlackBox with HasBlackBoxInline {
  val io = IO(new AudioPLLDriverIO())

  setInline("AudioPLLDriver.v",
    s"""
       |module AudioPLLDriver(
       |    input clock,
       |    input reset,
       |    output c0
       |    );
       |
       |    AudioPLL AudioPLL_1 (
       |      .inclk0(clock),
       |      .areset(reset),
       |      .c0(c0),
       |      .locked()
       |    );
       |endmodule
       |""".stripMargin
  )
}
