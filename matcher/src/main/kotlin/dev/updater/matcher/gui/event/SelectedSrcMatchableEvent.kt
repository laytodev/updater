package dev.updater.matcher.gui.event

import dev.updater.matcher.asm.Matchable
import tornadofx.FXEvent

class SelectedSrcMatchableEvent(val src: Matchable<*>) : FXEvent()