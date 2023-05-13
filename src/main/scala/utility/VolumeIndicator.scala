package utility

import chisel3._
import chisel3.util.{Counter, log2Up}
import io.{LEDIO}
import utility.Constants._

// Converts binary number to BCD
class VolumeIndicator extends Module {
  val io = IO(new Bundle {
    val ledio = new LEDIO
    val data = Input(SInt(DATA_WIDTH.W))
  })

  val rledReg = Reg(Vec(18, Bool()))

  for (i <- 0 until 18) {
    io.ledio.rled(i) := rledReg(i)
  }
  // default values..
  for (i <- 0 until 9) {
    io.ledio.gled(i) := false.B
  }

  // use rldeds to display current input, 20 times a second
  val (_, counterWrap) = Counter(true.B, CYCLONE_II_FREQ / 20)
  val maxLevelReg = RegInit(0.S(DATA_WIDTH.W))
  when (io.data > maxLevelReg) {
    maxLevelReg := io.data
  }
  when (counterWrap) {
    // we get 24 bit signed values, thus abs(data) is in [0, 2^23)
    // we can output 18 levels
    // let us output the range [2^13 + 2^12, 2^22 + 2^21) in logarithmic half steps
    // meaning led i will be on if
    //   if i is even: if maxLevel is >= 2^(22 - i/2)
    //   if i is odd: if maxLevel is >= 2^(22 - (i+1)/2) + 2^(22 - (i+1)/2 - 1)
    for (i <- 0 until 18) {
      val cond =
          if (i % 2 == 0) {
            (maxLevelReg >= scala.math.pow(2, 22 - i / 2).toLong.S)
          } else {
            (maxLevelReg >= (scala.math.pow(2, 22 - (i+1) / 2).toLong.S) + scala.math.pow(2, 22 - (i+1) / 2 - 1).toLong.S)
          }
      when (cond) {
        rledReg(i) := true.B
      } .otherwise {
        rledReg(i) := false.B
      }
    }
    maxLevelReg := 0.S
  }

}
