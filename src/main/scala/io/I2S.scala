package io

import chisel3._
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.Constants._
import utility.Errors._

class I2SIO(isOutput: Int, channelWidth: Int) extends Bundle {
  // pins
  val lrc = Input(Bool()) // left or right
  val bclk = Input(Bool()) // we operate in slave mode
  val dat = if (isOutput == 0) {
    Input(Bool())
  } else {
    Output(Bool())
  }

  // one for each channel
  val data = Vec(
    2,
    if (isOutput == 0) {
      Output(UInt(channelWidth.W))
    } else {
      Input(UInt(channelWidth.W))
    }
  )
  // true when updating
  val sync = Output(Bool())
}

// Actually not exactly I2S but rather DSP mode
// Either I2S(0, 24) to get data from WM8731 or I2S(1, 24) to set data to WM8731
class I2S(isOutput: Int, channelWidth: Int) extends Module {
  val io = IO(new I2SIO(isOutput, channelWidth))

  // double sample for cross clock boundary
  val bclkReg = RegNext(io.bclk)
  val lrcReg = RegNext(io.lrc)

  val lrc_posedge = lrcReg & RegNext(~lrcReg, false.B)
  val lrc_negedge = ~lrcReg & RegNext(lrcReg, false.B)
  val bclk_posedge = bclkReg & RegNext(~bclkReg, false.B)
  val bclk_negedge = ~bclkReg & RegNext(bclkReg, false.B)

  val tempDataReg = Reg(Vec(2, UInt(channelWidth.W)))
  val syncReg = RegInit(false.B)
  io.sync := syncReg

  // synced with tempDataReg on lrc posedge to ensure consistent values
  val dataReg = Reg(Vec(2, UInt(channelWidth.W)))
  val datReg = RegInit(false.B)
  if (isOutput == 0) {
    io.data <> dataReg
    // double sample for cross clock boundary
    datReg := RegNext(io.dat)
  } else {
    dataReg <> io.data
    // io.dat := datReg
  }

  val channelReg = RegInit(0.U(1.W))
  val bitsLeftReg = RegInit(0.U(6.W))
  val lastLrcOnBclkPosedgeReg = RegInit(false.B)

  syncReg := false.B

  when(bclk_posedge) {
    when(lrcReg) {
      // in mid sync
      dataReg(0) := tempDataReg(0)
      dataReg(1) := tempDataReg(1)
      // signal that data is ready, use register to ensure registers already updated
      syncReg := true.B

      bitsLeftReg := channelWidth.U
      channelReg := 0.U // left channel
    }.otherwise {
      // not in lrc, reading data
      if (isOutput == 0) {
        when(bitsLeftReg > 0.U) {
          tempDataReg(channelReg) := tempDataReg(channelReg)(
            channelWidth - 2,
            0
          ) ## datReg
          when(channelReg === 0.U && bitsLeftReg === 1.U) {
            channelReg := 1.U
            bitsLeftReg := channelWidth.U
          }.otherwise {
            bitsLeftReg := bitsLeftReg - 1.U
          }
        }
      }
    }
  }

  if (isOutput == 1) {
    val dontChange = RegInit(false.B)
    // lrc posedge is 2 clocks delayed, so we are here about in the middle of it
    // so we are somewhere around posedge of bclk with high lrc
    when(lrc_posedge) {
      tempDataReg(0) := dataReg(0)
      tempDataReg(1) := dataReg(1)
      bitsLeftReg := channelWidth.U
      channelReg := 0.U // left channel
      dontChange := true.B
    }
    // we change on blck posedge because it is 2 clocks delayed, thus is somewhere
    // around negedge actually..

    // we next time we bclk_posedge must be after lrc_posedge, and we are actually
    // beause of 2 ticks delay at negedge, so time to change anyway..
    when(bclk_posedge && bitsLeftReg > 0.U) {
      when(dontChange) {
        dontChange := false.B
      }.otherwise {
        // if we finished with the first channel, switch to the second
        when(channelReg === 0.U && bitsLeftReg === 1.U) {
          channelReg := 1.U
          bitsLeftReg := channelWidth.U
        }.otherwise {
          bitsLeftReg := bitsLeftReg - 1.U
        }
      }
    }

    when(bitsLeftReg > 0.U) {
      io.dat := tempDataReg(channelReg)(bitsLeftReg - 1.U)
    }.otherwise {
      io.dat := false.B
    }
  }
}
