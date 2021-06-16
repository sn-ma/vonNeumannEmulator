package snma.neumann

object Constants {
    object Model {
        const val BITS_IN_COMMAND_FOR_EACH_ADDRESSING = 4
        const val BITS_IN_NORMAL_CELL = 16
        const val BITS_IN_FLAGS_MEM_CELL = 2
    }

    object View {
        const val MEMORY_CELLS_PER_ROW = 8
        const val FONT_SIZE_BIG = 20.0
        const val BUTTONS_SPACING = 5.0
    }

    object Utils {
        object Saver { // Remember to fix tests when editing this values
            const val REMOVE_EMPTY_CELLS_MIN_COUNT = 8
            const val MAX_CELLS_IN_LINE = 16
        }
    }
}