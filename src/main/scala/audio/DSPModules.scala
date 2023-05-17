package audio

import chisel3._

class DSPModules {
  // Specify which effects to use
  var effects: List[DSPModule] = List(
    Module(new NoiseGate(0xc0, true, 16)),
    Module(new ClampDistortion(16, true, 16)),
    Module(new MovingAverage(16, true)),
    Module(new VolumeControl(32, true))
  )
}

object DSPModules {
  def apply: DSPModules = {
    var d = new DSPModules
    d
  }
}
