package audio

import utility.Constants.DATA_WIDTH

import chisel3._
import chisel3.util._
import scala.math._

class Distortion(defaultCtrl: Int = 10) extends DSPModule(defaultCtrl) {
	val lookupBits: Int = 10
	val gainWidth: Int = 6
	val maxGain: Double = 250.0

  val absDataWidth = DATA_WIDTH - 1
  val maxSignal = (1 << absDataWidth) - 1 // max input value

  val gainDataWidth = absDataWidth + gainWidth
  val maxGainSignal = (1 << gainDataWidth) - 1 // max input value with gain

  val fractBits = gainDataWidth - lookupBits // determine LU steps
  val lookupSteps = 1 << fractBits

	val gainSteps = (1 << gainWidth).toDouble
  val gainConst = maxGain / gainSteps

	val lookupValues = Range(lookupSteps - 1, maxGainSignal + 1, lookupSteps).map(i => maxSignal * (1.0 - scala.math.exp(-gainConst * i.toDouble / maxSignal.toDouble)))
	val lookupTable = VecInit(lookupValues.map(v => scala.math.round(v).asUInt(absDataWidth.W)))

	val inVal = audioInReg
	val inValAbs = inVal.abs.asUInt.min(maxSignal.U).tail(1)
	val regInValSign = ShiftRegister(inVal(DATA_WIDTH - 1), 2) // delay by two stages

	val gain = ctrlReg // get gain value from control signal
	val gainMul = inValAbs * gain
	val regInValGain = RegNext(gainMul)

	val lookupIndex = regInValGain >> fractBits
	val lookupFraction = regInValGain(fractBits - 1, 0) // part to be preserved

	val lookupLow = WireDefault(0.U(absDataWidth.W))
  when(lookupIndex === 0.U) {
    lookupLow := 0.U
  }.otherwise {
    lookupLow := lookupTable(lookupIndex - 1.U)
  }
  val lookupHigh = lookupTable(lookupIndex)
  val lookupDiff = lookupHigh - lookupLow
  val regLookupLow = RegInit(0.U(absDataWidth.W))

	val interpMul = lookupDiff * lookupFraction
	val regInterp = RegInit(0.U(absDataWidth.W))


  // Output
  when(regInValSign === true.B) {
    io.audioOut := -(regInterp +& regLookupLow).asSInt()
  } .otherwise {
    io.audioOut := (regInterp +& regLookupLow).asSInt()
  }
 
	regLookupLow := lookupLow
	regInterp := interpMul >> fractBits
}