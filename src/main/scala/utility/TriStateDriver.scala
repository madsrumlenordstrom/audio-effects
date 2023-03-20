package utility

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.HasBlackBoxInline

// This module allows to connect to tri-state busses with Chisel by using a Verilog blackbox.
class TriStateDriver(width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new TriStateDriverIO(width))

  setInline("LCDBusDriver.v",
    s"""
       |module LCDBusDriver(
       |    output [${width-1}:0] busData,
       |    input [${width-1}:0] driveData,
       |    inout [${width-1}:0] bus,
       |    input drive);
       |
       |    assign bus = drive ? driveData : {(${width}){1'bz}};
       |    assign busData = bus;
       |endmodule
       |""".stripMargin
  )
}
