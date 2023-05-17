package audio

import chisel3._

class Compressor(defaultCtrl: Int = 16, threshholdFrac: Int = 128)
    extends DSPModule(defaultCtrl) {}
