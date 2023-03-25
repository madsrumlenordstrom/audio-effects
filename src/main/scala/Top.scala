import chisel3._

import io.{WM8731Controller,WM8731IO}
import io.{LEDController,LEDIO}

class Top() extends Module {
  val io = IO(new Bundle {
    val ledio = new LEDIO
    val wm8731io = new WM8731IO
  })

  val led_controller = Module(new LEDController())
  led_controller.io.ledio <> io.ledio
  led_controller.io.error := false.B
  led_controller.io.errorCode := 0.U

  // gled0 - on
  io.ledio.gled(0) := true.B
  
  val wm8731_controller = Module(new WM8731Controller())
  // connect pins from top module to controller module
  wm8731_controller.io.wm8731io <> io.wm8731io

  // gled1 indicates whether wm8731 ready
  io.ledio.gled(1) := wm8731_controller.io.ready

  // blinking rled0 indicates wm8731 error
  when (wm8731_controller.io.error) {
    led_controller.io.error := true.B
    led_controller.io.errorCode := wm8731_controller.io.errorCode
  }

}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(), args)
}
