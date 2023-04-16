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

// Either I2S(0, 24) to get data from WM8731 or I2S(1, 24) to set data to WM8731
class I2S(isOutput: Int, channelWidth: Int) extends Module {
  val io = IO(new I2SIO(isOutput, channelWidth))

  val lrc_posedge = io.lrc & RegNext(~io.lrc, false.B)
  val lrc_negedge = ~io.lrc & RegNext(io.lrc, false.B)
  val bclk_posedge = io.bclk & RegNext(~io.bclk, false.B)
  val bclk_negedge = ~io.bclk & RegNext(io.bclk, false.B)

  val datReg = RegInit(false.B)
  // msb is ignored in protocol
  val tempDataReg = Reg(Vec(2, UInt((channelWidth + 1).W)))
  val syncReg = RegInit(false.B)
  io.sync := syncReg

  // synced with tempDataReg on lrc posedge to ensure consistent values
  val dataReg = Reg(Vec(2, UInt(channelWidth.W)))
  if (isOutput == 0) {
    io.data <> dataReg
    datReg := io.dat
  } else {
    dataReg <> io.data
    io.dat := datReg
  }

  val channel = RegInit(0.U(1.W))
  // counted from msb to lsb, never overlap
  val currentTick = RegInit(0.U(32.W))
  when (bclk_negedge) {
    currentTick := currentTick + 1.U
  }

  when (lrc_posedge) {
    // sync frames on lrc posedge
    if (isOutput == 0) {
      dataReg(0) := tempDataReg(0)(channelWidth - 1, 0)
      dataReg(1) := tempDataReg(1)(channelWidth - 1, 0)
    } else {
      tempDataReg(0) := 0.U(1.W) ## dataReg(0)
      tempDataReg(1) := 0.U(1.W) ## dataReg(1)
    }
    // signal that data is ready, use register to ensure registers already updated
    syncReg := true.B

    currentTick := 0.U
    channel := 0.U // left channel
  } .otherwise {
    syncReg := false.B
  }

  when (lrc_negedge) {
    currentTick := 0.U
    channel := 1.U // right channel
  }

  if (isOutput == 0) {
    // bclk posedge shouldn't ever happen on lrc posedge
    // so all registers' values should be already updated
    when (bclk_posedge) {
      when (currentTick <= channelWidth.U) {
        tempDataReg(channel) := tempDataReg(channel)(channelWidth - 1, 0) ## datReg
      }
    }
  }
 
  if (isOutput == 1) {
    val changeOutput = RegInit(false.B)
    // update output dat one clock after bclk negedge so all regs are synced
    when (bclk_negedge) {
      changeOutput := true.B
    }
    when (changeOutput) {
      changeOutput := false.B
      when (currentTick <= channelWidth.U) {
        datReg := tempDataReg(channel)(channelWidth.U - currentTick) // take msb
      }
    }
  }
}
