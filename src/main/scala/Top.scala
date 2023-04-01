import chisel3._

import io.{WM8731Controller,WM8731IO}
import io.{LEDController,LEDIO}

class Top() extends Module {
  val io = IO(new Bundle {
    val ledio = new LEDIO
    val wm8731io = new WM8731IO
  })

  withReset(!reset.asBool) {
    val ledCtrl = Module(new LEDController())
    ledCtrl.io.ledio <> io.ledio
    ledCtrl.io.error := false.B
    ledCtrl.io.errorCode := 0.U

    // gled8 - on
    io.ledio.gled(8) := true.B
    
    val wm8731Ctrl = Module(new WM8731Controller())
    // connect pins from top module to controller module
    wm8731Ctrl.io.wm8731io <> io.wm8731io

    // gled0 indicates whether wm8731 ready
    io.ledio.gled(0) := wm8731Ctrl.io.ready

    // blinking rled0 indicates wm8731 error
    when (wm8731Ctrl.io.error) {
      ledCtrl.io.error := true.B
      ledCtrl.io.errorCode := wm8731Ctrl.io.errorCode
    }
  }
}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(), args)
}
