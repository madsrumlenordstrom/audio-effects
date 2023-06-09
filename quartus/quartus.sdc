## Generated SDC file "quartus.sdc"

## Copyright (C) 1991-2013 Altera Corporation
## Your use of Altera Corporation's design tools, logic functions 
## and other software and tools, and its AMPP partner logic 
## functions, and any output files from any of the foregoing 
## (including device programming or simulation files), and any 
## associated documentation or information are expressly subject 
## to the terms and conditions of the Altera Program License 
## Subscription Agreement, Altera MegaCore Function License 
## Agreement, or other applicable license agreement, including, 
## without limitation, that your use is for the sole purpose of 
## programming logic devices manufactured by Altera and sold by 
## Altera or its authorized distributors.  Please refer to the 
## applicable agreement for further details.


## VENDOR  "Altera"
## PROGRAM "Quartus II"
## VERSION "Version 13.0.1 Build 232 06/12/2013 Service Pack 1 SJ Web Edition"

## DATE    "Sun May  7 23:01:58 2023"

##
## DEVICE  "EP2C70F896C6"
##


#**************************************************************
# Time Information
#**************************************************************

set_time_format -unit ns -decimal_places 3



#**************************************************************
# Create Clock
#**************************************************************

create_clock -name {io_clock50} -period 20.000 -waveform { 0.000 10.000 } [get_ports {io_clock50}]
create_clock -name {clock} -period 20.000 -waveform { 0.000 10.000 } [get_ports {clock}]
create_clock -name {bclk} -period 83.333 [get_ports {io_wm8731io_bclk}]


#**************************************************************
# Create Generated Clock
#**************************************************************

create_generated_clock -name {wm8731Ctrl|audioPLL|AudioPLL_1|altpll_component|pll|clk[0]} -source [get_pins {wm8731Ctrl|audioPLL|AudioPLL_1|altpll_component|pll|inclk[0]}] -duty_cycle 50.000 -multiply_by 6 -divide_by 25 -master_clock {io_clock50} [get_pins {wm8731Ctrl|audioPLL|AudioPLL_1|altpll_component|pll|clk[0]}] 


derive_pll_clocks

#**************************************************************
# Set Clock Latency
#**************************************************************



#**************************************************************
# Set Clock Uncertainty
#**************************************************************

derive_clock_uncertainty

set_clock_groups \
    -asynchronous \
    -group {{wm8731Ctrl|audioPLL|AudioPLL_1|altpll_component|pll|clk[0]} {io_clock50} {clock}} \
    -group {bclk}


#**************************************************************
# Set Input Delay
#**************************************************************

set_input_delay -clock bclk -max 4 [get_ports {io_wm8731io_adc_adclrck}]
set_input_delay -clock bclk -min -1 [get_ports {io_wm8731io_adc_adclrck}]
set_input_delay -clock bclk -max 4 [get_ports {io_wm8731io_adc_adcdat}]
set_input_delay -clock bclk -min -1 [get_ports {io_wm8731io_adc_adcdat}]
set_input_delay -clock bclk -max 4 [get_ports {io_wm8731io_dac_daclrck}]
set_input_delay -clock bclk -min -1 [get_ports {io_wm8731io_dac_daclrck}]

#**************************************************************
# Set Output Delay
#**************************************************************

set_output_delay -clock bclk -max 6 [get_ports {io_wm8731io_dac_dacdat}]
set_output_delay -clock bclk -min -3 [get_ports {io_wm8731io_dac_dacdat}]


#**************************************************************
# Set Clock Groups
#**************************************************************



#**************************************************************
# Set False Path
#**************************************************************



#**************************************************************
# Set Multicycle Path
#**************************************************************



#**************************************************************
# Set Maximum Delay
#**************************************************************



#**************************************************************
# Set Minimum Delay
#**************************************************************



#**************************************************************
# Set Input Transition
#**************************************************************

