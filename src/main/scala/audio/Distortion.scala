package audio

import utility.Constants.DATA_WIDTH

import chisel3._
import chisel3.util._
import scala.math._

class Distortion() extends DSPModule {
	val lookupBits: Int = 10
	val gainWidth: Int = 6
	val maxGain: Double = 250.0

  val absDataWidth = DATA_WIDTH - 1
  val maxSignal = (1 << absDataWidth) - 1 // input

  val gainDataWidth = absDataWidth + gainWidth
  val maxGainSignal = (1 << gainDataWidth) - 1 // input with gain

  val fractBits = gainDataWidth - lookupBits // determine LU steps
  val lookupSteps = 1 << fractBits

	val gainSteps = (1 << gainWidth).toDouble
  val gainConst = maxGain / gainSteps

	val lookupValues = Range(lookupSteps - 1, maxGainSignal + 1, lookupSteps).map(i => maxSignal * (1.0 - scala.math.exp(-gainConst * i.toDouble / maxSignal.toDouble)))
	val lookupTable = VecInit(lookupValues.map(v => scala.math.round(v).asUInt(absDataWidth.W)))


  val idle :: distort :: hasValue :: Nil = Enum(3)
  val regState = RegInit(idle) // initialize as idle state

	val inVal = audioInReg
	val inValAbs = inVal.abs.asUInt.min(maxSignal.U).tail(1)
	val regInValSign = ShiftRegister(inVal(DATA_WIDTH - 1), 2) // delay by two stages

	val gain = io.ctrlSig // get gain value from control signal
	val gainMul = inValAbs * gain
	val regInValGain = RegNext(gainMul)

	val lookupIndex = regInValGain >> fractBits
	val lookupFraction = regInValGain(fractBits - 1, 0) // part to be preserved

	val lookupLow = WireDefault(0.U(absDataWidth.W))
  when(lookupIndex === 0.U) { // 0-index is excluded so we mux.
    lookupLow := 0.U
  }.otherwise {
    lookupLow := lookupTable(lookupIndex - 1.U) // lower bound
  }
  val lookupHigh = lookupTable(lookupIndex) // upper bound
  val lookupDiff = lookupHigh - lookupLow
  val regLookupLow = RegInit(0.U(absDataWidth.W)) // why assign register for lookup low?
	// why assign the register's value in FSM?

	val interpMul = lookupDiff * lookupFraction
	val regInterp = RegInit(0.U(absDataWidth.W))


  // Output Code.
  when(regInValSign === true.B) {
    io.audioOut := -(regInterp +& regLookupLow).asSInt()
  } .otherwise {
    io.audioOut := (regInterp +& regLookupLow).asSInt()
  }
 
	regLookupLow := lookupLow
	regInterp := interpMul >> fractBits
}