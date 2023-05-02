package audio

import utility.Constants.{CTRL_WIDTH, DATA_WIDTH}

import chisel3._

// Generalized FIR filter parameterized by the convolution coefficients
class FIRFilter(coeffs: Seq[SInt]) extends DSPModule {
  // Create the serial-in, parallel-out shift register
  val zs = Reg(Vec(coeffs.length, SInt(DATA_WIDTH.W)))
  when (io.clk) {
    zs(0) := audioInReg
  }
  for (i <- 1 until coeffs.length) {
    when (io.clk) {
      zs(i) := zs(i-1)
    }
  }

  // Do the multiplies
  val products = VecInit.tabulate(coeffs.length)(i => zs(i) * coeffs(i))

  // Sum up the products
  io.audioOut := products.reduce(_ + _)

  when (io.ctrlSig(CTRL_WIDTH - 1) === 1.U) {
    io.audioOut := audioInReg
  }
}
