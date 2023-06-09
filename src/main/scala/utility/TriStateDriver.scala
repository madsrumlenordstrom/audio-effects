package utility

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.HasBlackBoxInline

// This module allows to connect to tri-state busses with Chisel by using a Verilog blackbox.
class TriStateBusDriver(width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new TriStateBusDriverIO(width))

  setInline(
    "TriStateBusDriver.v",
    s"""
       |module TriStateBusDriver(
       |    output [${width - 1}:0] in,
       |    input [${width - 1}:0] out,
       |    inout [${width - 1}:0] bus,
       |    input drive);
       |
       |    assign bus = drive ? (out ? 1'bz : 1'b0) : {(${width}){1'bz}};
       |    assign in = bus;
       |endmodule
       |""".stripMargin
  )
}
