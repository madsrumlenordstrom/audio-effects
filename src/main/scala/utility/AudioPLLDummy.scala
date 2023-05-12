package utility

import chisel3._
import chisel3.util.HasBlackBoxInline

// This is a dummy module for the quartus ip to make tests happy
// AudioPLLDummy.v should not be included in the quartus project

class AudioPLL() extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val inclk0 = Input(Clock())
    val areset = Input(Bool())
    val c0 = Output(Bool())
    val locked = Output(Bool())
  })
  setInline(
    "AudioPLLDummy.v",
    s"""
       |module AudioPLL(
       |    input inclk0,
       |    input areset,
       |    output c0,
       |    output locked
       |    );
       |
       |endmodule
       |""".stripMargin
  )
}
