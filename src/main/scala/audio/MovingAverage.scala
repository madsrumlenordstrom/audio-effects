package audio

import chisel3._
import chisel3.util.log2Ceil
import scala.math.pow

import utility.Constants.{CTRL_WIDTH}

object MovingAverage {
  // Function to calculate coefficients for an moving average FIR filter
  def movingAvgSeq(n: Int): Seq[SInt] = {
    val coeff = (pow(2, CTRL_WIDTH / 2).toInt / n)
    var seq = Seq[SInt]()
    for (i <- 0 until n) {
      seq = seq :+ coeff.S
    }
    return seq
  }
}

import MovingAverage.movingAvgSeq

class MovingAverage(length: Int = 16) extends FIRFilter(movingAvgSeq(length)) {}
