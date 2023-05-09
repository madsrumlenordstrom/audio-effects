package audio

import chisel3._

object DSPModules {
  // Specify which effects to use
  val effects = List(
    Module(new ClampDistortion(16)),
    //Module(new VolumeControl(0x0000)),
    Module(new VolumeControl(16)),
    //Module(new FIRFilter(Seq(1.S,2.S,3.S))),
  )
}
