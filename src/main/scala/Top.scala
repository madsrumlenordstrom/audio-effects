import chisel3._
import chisel3.util.Counter

import utility.Constants._
import io.{WM8731Controller,WM8731IO}

class Top(freq: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val led0 = Output(Bool())
    val wm8731io = new WM8731IO
  })
  // Blink LED every second using Chisel built-in util.Counter
  val led = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, freq / 2)
  when(counterWrap) {
    led := ~led
  }
  io.led0 := led

  val wm8731_controller = Module(new WM8731Controller())
  // connect pins from top module to controller module
  wm8731_controller.io.wm8731io <> io.wm8731io

}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(CYCLONE_II_FREQ), args)
}
