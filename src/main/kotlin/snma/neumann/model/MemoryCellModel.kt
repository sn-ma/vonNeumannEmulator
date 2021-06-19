package snma.neumann.model

import snma.neumann.Constants
import kotlin.math.ceil

class MemoryCellModel(
    val type: Type
) : AbstractCellModel<Int>(0) {
    enum class Type(val bitsCount: Int) {
        DATA_CELL(Constants.Model.BITS_IN_NORMAL_CELL),
        ADDRESS_CELL(Constants.Model.BITS_IN_NORMAL_CELL),
        FLAGS_CELL(Constants.Model.BITS_IN_FLAGS_MEM_CELL),
        ;

        val bytesCount = ceil(bitsCount / 8.0).toInt()
        val bitmask = (-1 shl bitsCount).inv()
    }

    init {
        valueBehaviorSubject.subscribe { newValue ->
            val fixedValue = newValue and type.bitmask
            if (fixedValue != newValue) {
                valueBehaviorSubject.onNext(fixedValue)
            }
        }
    }
}