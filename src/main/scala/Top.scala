import chisel3._
import chisel3.util.{Counter, log2Up}

import io.{WM8731Controller,WM8731IO}
import io.{LEDController,LEDIO}
import audio.{AudioProcessingFrame,AudioProcessingFrameIO, DSPModules}
import utility.Constants._

/// Switches:       LOW                     HIGH
/// SW0          combine channels       select one channel
/// SW1              -                  connect dacdat to adcdat, bypass decoder
/// SW2          select left chan       select right chan
/// SW3              -                  bypass DSP module
/// SW4
/// SW5
/// SW6
/// SW7             INVALID                 INVALID         (not connected)

class Top() extends Module {
  val io = IO(new Bundle {
    val clock50 = Input(Bool())
    val ledio = new LEDIO
    val wm8731io = new WM8731IO
    val sw = Vec(18, Input(Bool())) // switches
    val dspWrite = Input(Bool())
  })

  withReset(!reset.asBool) {
    val ledCtrl = Module(new LEDController())
    ledCtrl.io.ledio <> io.ledio
    ledCtrl.io.error := false.B
    ledCtrl.io.errorCode := 0.U

    // TODO: remove
    // gled8 - on
    io.ledio.gled(8) := true.B
    //io.ledio.gled(1) := io.wm8731io.i2c.sclk
    //io.ledio.gled(2) := ~io.wm8731io.i2c.sclk
    //io.ledio.gled(3) := io.wm8731io.xck
    //io.ledio.gled(4) := ~io.wm8731io.xck
    io.ledio.gled(5) := io.wm8731io.adc.adclrck
    io.ledio.gled(6) := ~io.wm8731io.adc.adclrck
    io.ledio.gled(7) := io.wm8731io.bclk
    io.ledio.gled(8) := ~io.wm8731io.bclk
    
    val wm8731Ctrl = Module(new WM8731Controller())
    wm8731Ctrl.io.clock50 := io.clock50
    // connect pins from top module to controller module
    wm8731Ctrl.io.wm8731io <> io.wm8731io
    wm8731Ctrl.io.combineChannels := io.sw(0)
    wm8731Ctrl.io.bypass := io.sw(1)
    wm8731Ctrl.io.channelSelect := io.sw(2)

    io.ledio.gled(1) := wm8731Ctrl.io.sync

    // TODO: move this connection to DSP module

    /// Connect to DSP Module
    val dsp = Module(new(AudioProcessingFrame))
    val addrWidth = log2Up(DSPModules.effects.length)
    
    // Connect addressing switches
    println("\n\nAddressing switches will be:")
    val dspAddr = Wire(Vec(addrWidth, UInt(1.W)))
    for (i <- io.sw.length - 1 until io.sw.length - addrWidth - 1 by -1) {
      print("SW" + i + " ")
      dspAddr(io.sw.length - i - 1) := io.sw(i).asUInt
    }
    // Connect control switches
    println("\n\nControl switches will be:")
    val dspCtrl = Wire(Vec(CTRL_WIDTH, UInt(1.W)))
    for (i <- io.sw.length - addrWidth - 1 until io.sw.length - addrWidth - CTRL_WIDTH - 1 by -1) {
      print("SW" + i + " ")
      dspCtrl(io.sw.length - i - 1 - addrWidth) := io.sw(i).asUInt
    }
    println("\n")
    dsp.io.write := io.dspWrite
    dsp.io.dspAddr := dspAddr.asUInt
    dsp.io.dspCtrl := dspCtrl.asUInt
    
    dsp.io.inData := wm8731Ctrl.io.inData
    dsp.io.clk := wm8731Ctrl.io.sync
    
    when (io.sw(3)) {
      // bypass dsp
      wm8731Ctrl.io.outData := wm8731Ctrl.io.inData
    } .otherwise {
      wm8731Ctrl.io.outData := dsp.io.outData
    }

    // TODO: move to a module
    val rledReg = Reg(Vec(18, Bool()))
    // use rldeds to display current input, 20 times a second
    val (_, counterWrap) = Counter(true.B, CYCLONE_II_FREQ / 20)
    val maxLevelReg = RegInit(0.S(24.W))
    when (wm8731Ctrl.io.inData > maxLevelReg) {
      maxLevelReg := wm8731Ctrl.io.inData
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

    // gled0 indicates whether wm8731 ready
    io.ledio.gled(0) := wm8731Ctrl.io.ready

    // blinking rled0 indicates wm8731 error
    when (wm8731Ctrl.io.error) {
      ledCtrl.io.error := true.B
      ledCtrl.io.errorCode := wm8731Ctrl.io.errorCode
    } .otherwise {
      for (i <- 0 until 18) {
        io.ledio.rled(i) := rledReg(i)
      }
    }
  }
}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(), args)
}
