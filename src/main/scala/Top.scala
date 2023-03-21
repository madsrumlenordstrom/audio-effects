import chisel3._
import chisel3.util.Counter
import utility.Constants._

class Top(freq: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val led0 = Output(Bool())
  })
  // Blink LED every second using Chisel built-in util.Counter
  val led = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, freq / 2)
  when(counterWrap) {
    led := ~led
  }
  io.led0 := led
}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(CYCLONE_II_FREQ), args)
}
