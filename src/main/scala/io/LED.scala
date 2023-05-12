package io

import chisel3._
import chisel3.util.Counter

import utility.Constants._

class LEDIO extends Bundle {
  val gled = Vec(9, Output(Bool()))
  val rled = Vec(18, Output(Bool()))
}

class LEDController extends Module {
  val io = IO(new Bundle {
    val ledio = new LEDIO

    // if error is true then red leds indicate 16 bit error code
    // and leftmost red led blinks
    val error = Input(Bool())
    val errorCode = Input(UInt(16.W))
  })

  // turn off all leds
  for (i <- 0 until 9) {
    io.ledio.gled(i) := WireDefault(false.B)
  }
  for (i <- 0 until 18) {
    io.ledio.rled(i) := WireDefault(false.B)
  }

  // Blink LED every second using Chisel built-in util.Counter
  val blinking_led = RegInit(false.B)
  val (_, counterWrap) = Counter(true.B, CYCLONE_II_FREQ / 2)
  when(counterWrap) {
    blinking_led := ~blinking_led
  }

  when(io.error) {
    io.ledio.rled(17) := blinking_led

    // display error code in red leds
    for (i <- 0 until 16) {
      io.ledio.rled(i) := ((io.errorCode >> i) & 1.U)
    }
  }
}
