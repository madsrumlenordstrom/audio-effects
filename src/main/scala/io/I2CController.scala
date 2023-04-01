package io

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.util.{switch, is, RegEnable, Counter}
import utility.{I2CIO,TriStateBusDriverIO}
import utility.Constants._
import utility.Errors._
import utility.ClockDividerByFreq
import utility.TriStateBusDriver

// Design roughly follow this description:
// https://www.ti.com/lit/an/slva704/slva704.pdf

object I2CController {
  // Note: currently supports write only
  object State extends ChiselEnum {
    val idle, start, startSdat, writeDeviceAddr, waitAckDeviceAddr, writeRegAddr, waitAckRegAddr, writeData, waitAckData, stop, error = Value
  }
}

// I2C Controller IO bundle
class I2CControllerIO extends Bundle {
  val i2cio = new I2CIO
  val start = Input(Bool())         // same semantics as reset, execution begins on negedge
  val regAddr = Input(UInt(8.W))
  val inData = Input(UInt(8.W))
  // val out = Output(UInt(8.W))
  // val read = Input(Bool())          // 1 for read (to in), 0 for write (out)

  val done = Output(Bool())
  val error = Output(Bool())
  val errorCode = Output(UInt(16.W))
}

// State machine for controlling an I2C bus
class I2CController(deviceAddr: Int, clockFreq: Int) extends Module {
  import I2CController.State
  import I2CController.State._

  val io = IO(new I2CControllerIO)

  // The TriState wrapper for sda
  val sda = Module(new TriStateBusDriver(1))
  // connect TriState wrapper to actual pin
  io.i2cio.sda <> sda.io.bus
  val sdaDriveReg = RegInit(true.B)
  val sdaOutReg = RegInit(0.U(1.W))
  sda.io.drive := sdaDriveReg
  sda.io.out := sdaOutReg

  val doneReg = Reg(Bool())
  val errorReg = Reg(Bool())
  val errorCodeReg = Reg(UInt(16.W))
  io.done := doneReg
  io.error := errorReg
  io.errorCode := errorCodeReg

  val outputClock = RegInit(false.B)

  val clockDivider = Module(new ClockDividerByFreq(CYCLONE_II_FREQ, clockFreq))
  // internal clock at clockFreq frequency
  val sclk = clockDivider.io.clk
  val sclk_negedge = ~sclk & RegNext(sclk, false.B)
  val sclk_posedge = sclk & RegNext(~sclk, false.B)

  when (outputClock) {
    io.i2cio.sclk := sclk
  } .otherwise {
    io.i2cio.sclk := true.B
  }

  // State machine register
  // we change state on internal clock's posedge, so all cases happen when state has changed
  val nextState = RegInit(idle)
  val stateReg = RegEnable(nextState, idle, sclk_posedge)
  // advance counter only on internal clock ticks
  val bitCounter = Reg(UInt(4.W))

  // same semantics as reset, on negedge
  //when (~io.start & RegNext(io.start, false.B)) {
  when (io.start) {
    doneReg := false.B
    errorReg := false.B
    errorCodeReg := 0.U
    // detach output clock from internal clock and set it to 1
    outputClock := false.B
    // output 1 on sda
    sdaOutReg := true.B
    sdaDriveReg := true.B
    stateReg := idle
    // skip at least one clock to margin from previous transaction
    nextState := start
  }

  // state machine works at sclk clock
  when (sclk_negedge) {
    switch (stateReg) {
      is (idle) {
        // output high on clock
        outputClock := false.B
        // loop in itself
      }
      is (start) {
        // this is just a buffer state to buffer from the previous transfer
        nextState := startSdat
      }
      is (startSdat) {
        // we do a falling sdat edge
        // but we don't connect the clock yet, so we'll get falling clock edge
        // only in the next state
        sdaOutReg := false.B
        nextState := writeDeviceAddr
        // reset the bit counter for next step
        bitCounter := 0.U
      }
      is (writeDeviceAddr) {
        // we connect the output sclk to internal sclk now so we get a falling edge on outer sclk
        outputClock := true.B

        sdaDriveReg := true.B
        when (bitCounter < 7.U) {
          sdaOutReg := (deviceAddr.U >> (6.U - bitCounter)) & 1.U
          bitCounter := bitCounter + 1.U
        } .otherwise {
          // last bit is r/w bit, we send read=0
          sdaOutReg := false.B
          // We've sent the last bit, advance to next stage
          nextState := waitAckDeviceAddr
        }
      }
      is (waitAckDeviceAddr) {
        // consists of two states - first we release the drive
        // then we read the is sda line
        when (sdaDriveReg) {
          sdaDriveReg := false.B
        } .otherwise {
          when (sda.io.in === 1.U) {
            // we got nack
            errorCodeReg := ERR_I2C_NACK1.U
            nextState := error
          } .otherwise {
            // we got ack
            nextState := writeRegAddr
            // reset the bit counter for next step
            bitCounter := 0.U
            sdaDriveReg := true.B
          }
        }
      }
      is (writeRegAddr) {
        sdaDriveReg := true.B
        when (bitCounter < 8.U) {
          sdaOutReg := (io.regAddr >> (7.U - bitCounter)) & 1.U
          bitCounter := bitCounter + 1.U
        } .otherwise {
          // We've sent the last bit, advance to next stage
          nextState := waitAckRegAddr
        }
      }
      is (waitAckRegAddr) {
        // consists of two states - first we release the drive
        // then we read the is sda line
        when (sdaDriveReg) {
          sdaDriveReg := false.B
        } .otherwise {
          when (sda.io.in === 1.U) {
            // we got nack
            errorCodeReg := ERR_I2C_NACK2.U
            nextState := error
          } .otherwise {
            // we got ack
            nextState := writeData
            // reset the bit counter for next step
            bitCounter := 0.U

            // force advancing the state in the next tick of the board clock
            // without waiting for next i2c sclk tick
            forceImmediateStateChangeReg := true.B
            // it is not enough to wait for next tick, cause otherwise stateReg will still have the old value
            // in the next switch
            stateReg := nextState
          }
        }
      }
      is (writeData) {
        sdaDriveReg := true.B
        when (bitCounter < 8.U) {
          sdaOutReg := (io.inData >> (7.U - bitCounter)) & 1.U
          bitCounter := bitCounter + 1.U
        } .otherwise {
          // We've sent the last bit, advance to next stage
          nextState := waitAckData
        }
      }
      is (waitAckData) {
        // consists of two states - first we release the drive
        // then we read the is sda line
        when (sdaDriveReg) {
          sdaDriveReg := false.B
        } .otherwise {
          when (sda.io.in === 1.U) {
            // we got nack
            errorCodeReg := ERR_I2C_NACK3.U
            nextState := error
          } .otherwise {
            // we got ack
            nextState := stop
          }
        }
      }
      is (stop) {
        // consists of two steps, first we do sda=0, clk=0
        // then we raise sda=1 and go to idle state which ensures clock doesn't go down
        when (~sdaDriveReg) {
          sdaOutReg := false.B
          sdaDriveReg := true.B
        } .otherwise {
          sdaOutReg := true.B
          // disconnect clock from internal clock so it doesn't go down
          outputClock := false.B
          doneReg := true.B
          nextState := idle
        }
      }
      is (error) {
        // loop in itself
        errorReg := true.B
      }
    }
  }
}
