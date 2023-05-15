package audio

import chisel3._

object DSPModules {
  // Specify which effects to use
  val effects = List(
    Module(new NoiseGate(0xC0, 16)),
    Module(new ClampDistortion(16, 16)),
    Module(new MovingAverage(16)),
    Module(new VolumeControl(32))
  )
}
