# audio-effects
Audio processing on a DE2-70 FPGA


![Diagram](docs/audio-effects-diagram.svg)

To generate verilog do:
```
make run
```

To run a simulation do:
```
make test
```
Note: Some tests require a sound file in the root directory named ```sample.wave```.

To quickly see waveforms from simulation do:
```
make wave
```
or simulate and waveforms in one command:
```
make test wave
```

More options for configuring make targets can be specified in a config.mk file. An example could be:
```
TESTTARGET = audio.AudioProcessingFrameSpec
WAVETARGET = test_run_dir/AudioProcessingFrame_should_play/AudioProcessingFrame.vcd
WAVECONFIG = test_run_dir/wave.gtkw
DIAGRAMTARGET = $(CURDIR)/build/FIRFilter.fir
DIAGRAMMERDIR = ~/repos/diagrammer
```

The signal path is defined in ```src/main/scala/audio/DSPModules.scala```:
```
object DSPModules {
  // Specify which effects to use
  val effects = List(
    Module(new ClampDistortion(16, 16384)),
    Module(new MovingAverage(16)),
    Module(new VolumeControl(8))
  )
}
```
This would create the signal path: FIRFilter -> ClampDistortion -> VolumeControl

To program the FPGA, plug in your DE2-70 development board and make sure you have quartus installed.
```
make program
```
Plug in an AUX cable to the ```LINE IN``` plug and either a speaker or headphones to the ```LINE OUT``` plug. Play some audio and you should hear it come through. If the audio does not come through right away you might need to press ```KEY 0``` (reset) a few times.