# audio-effects
Audio processing on a DE2-115 FPGA


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
    Module(new FIRFilter(Seq(1.S,2.S,3.S))),
    Module(new ClampDistortion(0x0000)),
    Module(new VolumeControl(0x0000)),
  )
}
```
This would create the signal path: FIRFilter -> ClampDistortion -> VolumeControl