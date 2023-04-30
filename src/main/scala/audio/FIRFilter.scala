package audio

import utility.Constants.DATA_WIDTH

import chisel3._

// Generalized FIR filter parameterized by the convolution coefficients
class FIRFilter(coeffs: Seq[UInt]) extends DSPModule {
  // Create the serial-in, parallel-out shift register
  val zs = Reg(Vec(coeffs.length, SInt(DATA_WIDTH.W)))
  zs(0) := io.audioIn
  for (i <- 1 until coeffs.length) {
    zs(i) := zs(i-1)
  }

  // Do the multiplies
  val products = VecInit.tabulate(coeffs.length)(i => zs(i) * coeffs(i))

  // Sum up the products
  io.audioOut := products.reduce(_ + _)
}
