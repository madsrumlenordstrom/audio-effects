all: build flash

flash:
	quartus_pgm -c 'USB-Blaster [1-3]' output_files/quartus.cdf

build:
	quartus_fit quartus
	quartus_asm quartus
	quartus_sta quartus
