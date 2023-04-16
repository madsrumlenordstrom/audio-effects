import chisel3._
import chisel3.util.Counter

import io.{WM8731Controller,WM8731IO}
import io.{LEDController,LEDIO}
import utility.Constants._

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

    io.ledio.gled(1) := io.wm8731io.bclk
    io.ledio.gled(2) := ~io.wm8731io.bclk
    io.ledio.gled(3) := io.wm8731io.xck
    io.ledio.gled(4) := ~io.wm8731io.xck
    io.ledio.gled(5) := io.wm8731io.adc.adclrck
    io.ledio.gled(6) := ~io.wm8731io.adc.adclrck
    
    val wm8731Ctrl = Module(new WM8731Controller())
    // connect pins from top module to controller module
    wm8731Ctrl.io.wm8731io <> io.wm8731io

    // TODO: move this connection to DSP module
    wm8731Ctrl.io.outData(0) := wm8731Ctrl.io.inData(0)
    // demonstrate single channel
    wm8731Ctrl.io.outData(1) := 0.S

    // TODO: move to a module
    val rledReg = Reg(Vec(18, Bool()))
    // use rldeds to display current input, 20 times a second
    val (_, counterWrap) = Counter(true.B, CYCLONE_II_FREQ / 20)
    val maxLevelReg = RegInit(0.S(24.W))
    when (wm8731Ctrl.io.inData(1) > maxLevelReg) {
      maxLevelReg := wm8731Ctrl.io.inData(1)
    }
    when (counterWrap) {
      for (i <- 0 until 18) {
          when (maxLevelReg >= scala.math.pow(2, 22 - i).toLong.S) {
            rledReg(i) := true.B
          } .otherwise {
            rledReg(i) := false.B
          }
      }
      maxLevelReg := 0.S
    }
    for (i <- 0 until 18) {
      io.ledio.rled(i) := rledReg(i)
    }

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
