# Import config file for user specific compiling and testing
-include config.mk

SBT = sbt
GTKWAVE ?= gtkwave

SRCDIR = $(CURDIR)/src
SCALADIR = $(SRCDIR)/main/scala
TESTDIR = $(SRCDIR)/test/scala
QUARTUSDIR = $(CURDIR)/quartus

# All sources and test sources
SRCS = $(shell find $(SCALADIR) -type f -name '*.scala')
TESTS = $(shell find $(TESTDIR) -type f -name '*.scala')

# Directories
HWBUILDDIR = $(CURDIR)/build
DIRS = $(SCALADIR) $(TESTDIR) $(HWBUILDDIR) $(GENERATED)

# Targets for verilog and testing
MAINTARGET ?= Main
TESTTARGET ?= Top
WAVETARGET ?= $(CURDIR)/
WAVECONFIG ?= # Use a config file to configure gtkwave
DIAGRAMTARGET ?=  $(CURDIR)/build/Top.fir
DIAGRAMMERDIR ?=

.PHONY: all
all: run

# Run main target
.PHONY: run
run: $(SRCS) dirs
	$(SBT) "runMain $(MAINTARGET) --target-dir $(HWBUILDDIR)"

# Run all tests
.PHONY: testall
testall: $(SRCS) $(TESTS)
	$(SBT) test

# Run specific test
.PHONY: test
test: $(SRCS) $(TESTS)
	$(SBT) "testOnly $(TESTTARGET)"

# Build and program FPGA
.PHONY: program
program: run
	$(MAKE) -C $(QUARTUSDIR)

# Program FPGA without building
.PHONY: flash
flash:
	$(MAKE) -C $(QUARTUSDIR) flash


# View waveform in GTKWave
.PHONY: wave
wave: test $(WAVETARGET)
	#if [ ! -f $(WAVETARGET) ]; then $(MAKE) test; fi # TODO make depend on TESTTARGET
	sed -ri 's/timescale .../timescale 10ns/g' $(WAVETARGET)
	$(GTKWAVE) $(WAVETARGET) $(WAVECONFIG) &

# Create directories if they don't exist
.PHONY: dirs
dirs:
	@echo "Creating directories"
	@for d in $(DIRS) ; do \
		mkdir -p $$d ; \
	done

# Create diagram using diagrammer
.PHONY: diagram
diagram:
	if [ ! -f $(DIAGRAMTARGET) ]; then $(MAKE) run; fi # TODO make depend on DIAGRAMTARGET
	cd $(DIAGRAMMERDIR); ./diagram.sh -i $(DIAGRAMTARGET) --target-dir $(HWBUILDDIR) --open-command xdg-open

# Cleanup working directory
.PHONY: clean
clean:
	@echo "Cleaning workspace"
	$(RM) -r *.v *.fir *.anno.json test_run_dir $(HWBUILDDIR) target project/target *.o *.bin *.txt

# Cleanup working directory included configuration files
.PHONY: veryclean
veryclean: clean
	$(RM) -r project/.bloop project/project project/metals.sbt .bloop .bsp .idea .metals .vscode


# Show variables for debugging
.PHONY: show
show:
	@echo 'MAKE         	:' $(MAKE)
	@echo 'CURDIR       	:' $(CURDIR)
	@echo 'SBT          	:' $(SBT)
	@echo 'SRCDIR       	:' $(SRCDIR)
	@echo 'SCALADIR     	:' $(SCALADIR)
	@echo 'TESTDIR      	:' $(TESTDIR)
	@echo 'SRCS         	:' $(SRCS)
	@echo 'TESTS        	:' $(TESTS)
	@echo 'HWBUILDDIR   	:' $(HWBUILDDIR)
	@echo 'DIRS         	:' $(DIRS)
	@echo 'MAINTARGET   	:' $(MAINTARGET)
	@echo 'TESTTARGET   	:' $(TESTTARGET)
	@echo 'WAVETARGET   	:' $(WAVETARGET)
	@echo 'WAVECONFIG   	:' $(WAVECONFIG)
	@echo 'GTKWAVE      	:' $(GTKWAVE)
	@echo 'DIAGRAMMERDIR	:' $(DIAGRAMMERDIR)
	@echo 'DIAGRAMTARGET	:' $(DIAGRAMTARGET)
