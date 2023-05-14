package audio

import scala.math._
import chisel3._
import chisel3.util._

import utility.Constants.DATA_WIDTH

/** Experimental octave down effect based on an indirect divider circuit.
  * Somewhat working version.
  *
  * Potential improvement: 1) Tweaking of regControl flipping logic and good
  * parameter set (based on real-world samples) is necessary 2) Filtering of the
  * output signal should help to eliminate artifacts.
  */
class OctaveDown() extends DSPModule {
  val bufferLength: Int = 32
  val maxIndex = bufferLength - 1
  val indexBits = log2Up(bufferLength)

  val filterCount: Int = 12
  val maxFilter = filterCount - 1
  val filterBits = log2Up(filterCount)

  val margin = 20

  // State Variables.
  val sampleCount = RegInit(0.U(indexBits.W))

  val regLowPeak = RegInit(0.S(DATA_WIDTH.W))
  val regLowPeakIndex = RegInit(0.U(indexBits.W))

  val regOffset = RegInit(0.S(DATA_WIDTH.W))

  val regRising = RegInit(0.U(filterBits.W))
  val regControl = RegInit(false.B)
  val regToggled = RegInit(false.B)

  // Input.
  val inVal = audioInReg

  // Buffer.
  val counter = Counter(bufferLength)
  val buffer =
    SyncReadMem(bufferLength, SInt(DATA_WIDTH.W), SyncReadMem.ReadFirst)
  val bufferSample = buffer(counter.value)

  // Output.
  val valOffset = bufferSample -& regOffset
  val valScaled = valOffset >> 1

  when(regControl) {
    audioOut := -valScaled
  }.otherwise {
    audioOut := valScaled
  }

  when(sampleCount < maxIndex.U) { // write to module when not full
    when((inVal > margin.S) & regToggled) { // clearing for next peak detection
      when(regRising < filterBits.U) {
        regRising := regRising + 1.U
      }.otherwise {
        regToggled := false.B
        regRising := 0.U
        regLowPeak := inVal
      }
    }

    when((regRising < filterBits.U) & !regToggled) { // peak detection
      when((inVal < -margin.S) & (inVal >= regLowPeak)) { // advance peak detection
        regRising := regRising + 1.U
      }.otherwise { // reset peak detection
        regRising := 0.U
        regLowPeak := inVal
        regLowPeakIndex := counter.value
      }
    }

    bufferSample := inVal
    sampleCount := sampleCount + 1.U
    counter.inc()
  }

  when(sampleCount === maxIndex.U) {
    when(
      (counter.value === regLowPeakIndex)
        & (regRising === filterBits.U)
        & !regToggled
    ) {
      regOffset := regLowPeak
      regControl := !regControl
      regRising := 0.U
      regToggled := true.B
    }

    sampleCount := sampleCount - 1.U
  }

}
