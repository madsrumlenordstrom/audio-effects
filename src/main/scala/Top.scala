import chisel3._
import chisel3.util.{Counter, log2Up}

import io.{WM8731Controller, WM8731IO}
import io.{LEDController, LEDIO}
import audio.{AudioProcessingFrame, AudioProcessingFrameIO, DSPModules}
import utility.Constants._
import utility.SevenSegDecoder
import utility.VolumeIndicator
import audio.Sounds

/// Switches:       LOW                     HIGH
/// SW0          combine channels       select single channel
/// SW1          select left chan       select right chan (needs sw0 = true)
/// SW2              -                  connect dacdat to adcdat, bypass decoder
/// SW3              -                  bypass all audio processing
/// SW4              -                  bypass currently selected DSP effect (on write edge)
/// SW5
/// SW6
/// SW7             INVALID                 INVALID         (not connected)

class Top(stereo: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clock50 = Input(Bool())
    val ledio = new LEDIO
    val wm8731io = new WM8731IO
    val sw = Vec(18, Input(Bool())) // switches
    val dspWrite = Input(Bool())
    val dspAddrSevSeg =
      Vec(2, Vec(7, Output(Bool()))) // seven segment addr display
    val dspCtrlSevSeg =
      Vec(2, Vec(7, Output(Bool()))) // seven segment ctrl display
    val dspStrdSevSeg =
      Vec(2, Vec(7, Output(Bool()))) // seven segment stored ctrl display
    val dspStrdBypass = Output(Bool()) // Stored bypass signal
    val dspBypass = Output(Bool()) // Current input bypass value
  })

  def connectAudioFrameControl(audioFrame: AudioProcessingFrame): Unit = {
    val addrWidth = log2Up(audioFrame.effects.length)
    // Connect addressing switches
    println("\n\nAddressing switches configured as:")
    val dspAddr = Wire(Vec(addrWidth, UInt(1.W)))
    for (i <- addrWidth - 1 to 0 by -1) {
      val swhIdx = io.sw.length - addrWidth + i
      print("SW" + swhIdx + " ")
      dspAddr(i) := io.sw(swhIdx).asUInt
    }

    // Connect control switches
    println("\n\nControl switches configured as:")
    val dspCtrl = Wire(Vec(CTRL_WIDTH, UInt(1.W)))
    for (i <- CTRL_WIDTH - 1 to 0 by -1) {
      val swhIdx = io.sw.length - addrWidth - CTRL_WIDTH + i
      print("SW" + swhIdx + " ")
      dspCtrl(i) := io.sw(swhIdx).asUInt
    }

    // Connect bypass switch
    println("\n\nBypass switch configured as:")
    val dspBypass = Wire(Bool())
    val swhIdx = 4
    dspBypass := io.sw(swhIdx).asBool
    print("SW" + swhIdx + " ")
    println("\n")

    // Send signal to audio frame
    audioFrame.io.write := ~io.dspWrite
    audioFrame.io.dspAddr := dspAddr.asUInt
    audioFrame.io.dspCtrl := dspCtrl.asUInt
    audioFrame.io.dspBypass := dspBypass
  }

  def connectAudioFrameStoredControl(audioFrame: AudioProcessingFrame): Unit = {
    // Seven segment display
    val dspAddrSevSeg = Module(new SevenSegDecoder)
    dspAddrSevSeg.io.sw := audioFrame.io.dspAddr
    for (i <- 0 until io.dspAddrSevSeg(0).length) {
      io.dspAddrSevSeg(0)(i) := dspAddrSevSeg.io.seg(i)
      io.dspAddrSevSeg(1)(i) := "b1111111".U // Maybe add later
    }

    // Control signal
    val dspCtrlSevSeg0 = Module(new SevenSegDecoder)
    val dspCtrlSevSeg1 = Module(new SevenSegDecoder)
    dspCtrlSevSeg0.io.sw := audioFrame.io.dspCtrl.asUInt(3, 0)
    dspCtrlSevSeg1.io.sw := audioFrame.io.dspCtrl.asUInt(7, 4)
    for (i <- 0 until io.dspCtrlSevSeg(0).length) {
      io.dspCtrlSevSeg(0)(i) := dspCtrlSevSeg0.io.seg(i)
      io.dspCtrlSevSeg(1)(i) := dspCtrlSevSeg1.io.seg(i)
    }

    // Stored control signal
    val dspStrdSevSeg0 = Module(new SevenSegDecoder)
    val dspStrdSevSeg1 = Module(new SevenSegDecoder)
    dspStrdSevSeg0.io.sw := audioFrame.io.strdCtrl.asUInt(3, 0)
    dspStrdSevSeg1.io.sw := audioFrame.io.strdCtrl.asUInt(7, 4)
    for (i <- 0 until io.dspStrdSevSeg(0).length) {
      io.dspStrdSevSeg(0)(i) := dspStrdSevSeg0.io.seg(i)
      io.dspStrdSevSeg(1)(i) := dspStrdSevSeg1.io.seg(i)
    }

    // Stored bypass signal
    io.dspStrdBypass := audioFrame.io.strdBypass
    io.dspBypass := audioFrame.io.dspBypass
  }

  withReset(!reset.asBool) {
    val ledCtrl = Module(new LEDController())
    ledCtrl.io.ledio <> io.ledio
    ledCtrl.io.error := false.B
    ledCtrl.io.errorCode := 0.U

    io.ledio.gled(8) := true.B
    io.ledio.gled(5) := io.wm8731io.adc.adclrck
    io.ledio.gled(6) := ~io.wm8731io.adc.adclrck
    io.ledio.gled(7) := io.wm8731io.bclk
    io.ledio.gled(8) := ~io.wm8731io.bclk

    val wm8731Ctrl = Module(new WM8731Controller())
    wm8731Ctrl.io.clock50 := io.clock50
    // connect pins from top module to controller module
    wm8731Ctrl.io.wm8731io <> io.wm8731io
    wm8731Ctrl.io.bypass := io.sw(2)

    io.ledio.gled(1) := wm8731Ctrl.io.sync

    // Connect to DSP Modules
    if (stereo) {
      val afs = Seq.fill(2)(Module(new AudioProcessingFrame))

      for (i <- 0 until afs.length) {
        afs(i).io.clk := wm8731Ctrl.io.sync
        afs(i).io.inData := wm8731Ctrl.io.inData(i)
        wm8731Ctrl.io.outData(i) := afs(i).io.outData
        connectAudioFrameControl(audioFrame = afs(i))
      }

      connectAudioFrameStoredControl(audioFrame = afs(0))

    } else {
      val af = Module(new (AudioProcessingFrame))
  
      af.io.clk := wm8731Ctrl.io.sync
      wm8731Ctrl.io.outData(0) := af.io.outData
      wm8731Ctrl.io.outData(1) := af.io.outData
  
      connectAudioFrameControl(audioFrame = af)
      connectAudioFrameStoredControl(audioFrame = af)
      // Channel selection logic
      when(io.sw(0)) {
        af.io.inData := wm8731Ctrl.io.inData(io.sw(1).asUInt).asSInt
      }.otherwise {
        // if combine channels, calculate mean value between left and right
        af.io.inData := ((wm8731Ctrl.io.inData(0).asSInt + wm8731Ctrl.io
          .inData(1)
          .asSInt) / 2.S).asSInt
      }
    }


    // gled0 indicates whether wm8731 ready
    io.ledio.gled(0) := wm8731Ctrl.io.ready

    val volumeIndicator = Module(new VolumeIndicator)
    // indicate the volume of what we are about to hear
    volumeIndicator.io.data := ((wm8731Ctrl.io.outData(0).asSInt + wm8731Ctrl.io
      .outData(1)
      .asSInt) / 2.S).asSInt

    // blinking rled0 indicates wm8731 error
    when(wm8731Ctrl.io.error) {
      ledCtrl.io.error := true.B
      ledCtrl.io.errorCode := wm8731Ctrl.io.errorCode
    }.otherwise {
      // forward the volume indication over the rleds
      for (i <- 0 until 18) {
        io.ledio.rled(i) := volumeIndicator.io.ledio.rled(i)
      }
    }
  }
}
object Main extends App {
  // Generate the Verilog output
  emitVerilog(new Top(stereo = true), args)
}
