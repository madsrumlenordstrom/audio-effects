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

To quickly see waveforms from simulation do:
```
make wave
```
or simulate and waveforms in one command:
```
make test wave
```

More options for configuring make targets can be specified in a config.mk file.