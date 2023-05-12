package audio

import chisel3._

import utility.Constants.{CTRL_WIDTH, DATA_WIDTH}

// Generalized FIR filter parameterized by the convolution coefficients
class FIRFilter(coeffs: Seq[SInt]) extends DSPModule(1) {
  // Create the serial-in, parallel-out shift register
  val zs = Reg(Vec(coeffs.length, SInt((DATA_WIDTH + (CTRL_WIDTH / 2)).W)))
  when(io.clk) {
    // Create fixed point representation
    zs(0) := (audioInReg ## 0.S((CTRL_WIDTH / 2).W)).asSInt
  }
  for (i <- 1 until coeffs.length) {
    when(io.clk) {
      zs(i) := zs(i - 1)
    }
  }

  // Do the multiplies
  val products = VecInit.tabulate(coeffs.length)(i =>
    ((zs(i) * coeffs(i).asTypeOf(SInt(CTRL_WIDTH.W))) >> (CTRL_WIDTH / 2))(
      DATA_WIDTH + (CTRL_WIDTH / 2) - 1,
      0
    ).asSInt
  )
  // Sum up the products
  io.audioOut := products
    .reduceTree(_ + _)(DATA_WIDTH + (CTRL_WIDTH / 2) - 1, CTRL_WIDTH / 2)
    .asSInt
}
