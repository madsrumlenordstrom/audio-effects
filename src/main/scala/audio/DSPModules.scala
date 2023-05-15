package audio

import chisel3._

class DSPModules {
  var effects: List[DSPModule] = List(
    Module(new NoiseGate(0xc0, 16)),
    Module(new ClampDistortion(16, 16)),
    Module(new MovingAverage(16)),
    Module(new VolumeControl(32))
  )
}

object DSPModules {
  // Specify which effects to use
  def apply: DSPModules = {
    var d = new DSPModules
    d
  }
}
