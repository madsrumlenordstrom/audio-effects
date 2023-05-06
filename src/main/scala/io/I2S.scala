package io

import chisel3._
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.Constants._
import utility.Errors._

class I2SIO(isOutput: Int, channelWidth: Int) extends Bundle {
  // pins
  val lrc = Input(Bool())           // left or right
  val bclk = Input(Bool())          // we operate in slave mode
  val dat = if (isOutput == 0) {
    Input(Bool())
  } else {
    Output(Bool())
  }

  // one for each channel
  val data = Vec(2, if (isOutput == 0) {
    Output(UInt(channelWidth.W))
  } else {
    Input(UInt(channelWidth.W))
  })
  // true when updating
  val sync = Output(Bool())
}

// Actually not exactly I2S but rather DSP mode
// Either I2S(0, 24) to get data from WM8731 or I2S(1, 24) to set data to WM8731
class I2S(isOutput: Int, channelWidth: Int) extends Module {
  val io = IO(new I2SIO(isOutput, channelWidth))

  val lrc_posedge = io.lrc & RegNext(~io.lrc, false.B)
  val lrc_negedge = ~io.lrc & RegNext(io.lrc, false.B)
  val bclk_posedge = io.bclk & RegNext(~io.bclk, false.B)
  val bclk_negedge = ~io.bclk & RegNext(io.bclk, false.B)

  //val datReg = RegInit(false.B)
  // msb is ignored in protocol
  val tempDataReg = Reg(Vec(2, UInt(channelWidth.W)))
  val syncReg = RegInit(false.B)
  io.sync := syncReg

  // synced with tempDataReg on lrc posedge to ensure consistent values
  val dataReg = Reg(Vec(2, UInt(channelWidth.W)))
  if (isOutput == 0) {
    io.data <> dataReg
    //datReg := io.dat
  } else {
    dataReg <> io.data
    //io.dat := datReg
  }

  val channelReg = RegInit(0.U(1.W))
  val bitsLeftReg = RegInit(0.U(5.W))
  val lastLrcOnBclkPosedgeReg = RegInit(false.B)

  when (bclk_posedge && io.lrc) {
    // in mid sync
    if (isOutput == 0) {
      dataReg(0) := tempDataReg(0)
      dataReg(1) := tempDataReg(1)
    } else {
      tempDataReg(0) := dataReg(0)
      tempDataReg(1) := dataReg(1)
    }
    // signal that data is ready, use register to ensure registers already updated
    syncReg := true.B

    bitsLeftReg := channelWidth.U
    channelReg := 0.U // left channel
  } .otherwise {
    syncReg := false.B
  }

  when (bclk_posedge) {
    lastLrcOnBclkPosedgeReg := io.lrc
  }

  if (isOutput == 0) {
    // bclk posedge shouldn't ever happen on lrc posedge or negedge
    // so all registers' values should be already updated
    when (bclk_posedge && !io.lrc) {
      when (bitsLeftReg > 0.U) {
        tempDataReg(channelReg) := tempDataReg(channelReg)(channelWidth - 2, 0) ## io.dat
        //bitsLeftReg := bitsLeftReg - 1.U
        when (channelReg === 0.U && bitsLeftReg === 1.U) {
          channelReg := 1.U
          bitsLeftReg := channelWidth.U
        } .otherwise {
          bitsLeftReg := bitsLeftReg - 1.U
        }
      }
    }
    // // will happen after bclk posedge because of bitLeft dec
    // when (channelReg === 0.U && bitsLeftReg === 0.U) {
    //   // second channel follows right after
    //   channelReg := 1.U
    //   bitsLeftReg := channelWidth.U
    // }
  }
 
  if (isOutput == 1) {
    // don't change if right after sync, the sync has already set it for us
    when (bclk_negedge && !lastLrcOnBclkPosedgeReg && bitsLeftReg > 0.U) {
      // if we finished with the first channel, switch to the second
      when (channelReg === 0.U && bitsLeftReg === 1.U) {
        channelReg := 1.U
        bitsLeftReg := channelWidth.U
      } .otherwise {
        bitsLeftReg := bitsLeftReg - 1.U
      }
    }

    when (bitsLeftReg > 0.U) {
      io.dat := tempDataReg(channelReg)(bitsLeftReg - 1.U)
    } .otherwise {
      io.dat := false.B
    }
  }
}
