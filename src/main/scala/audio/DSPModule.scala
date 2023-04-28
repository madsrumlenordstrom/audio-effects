package audio

import chisel3._
import utility.DSPModuleIO

class DSPModule extends Module {
  val io = IO(new DSPModuleIO())
}
