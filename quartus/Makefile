all: build flash

flash:
	quartus_pgm -c USB-Blaster -m JTAG -o "P;output_files/quartus.sof"

build:
	quartus_map quartus
	quartus_fit quartus
	quartus_asm quartus
	#quartus_sta quartus
