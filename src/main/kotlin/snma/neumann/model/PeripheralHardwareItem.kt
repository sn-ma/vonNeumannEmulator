package snma.neumann.model

import snma.neumann.CommonUtils.countValuableBits
import snma.neumann.Constants

abstract class PeripheralHardwareItem(
    busModel: BusModel,
    val addressRange: IntRange,
): HardwareItem(busModel) {
    init {
        check(addressRange.first.countValuableBits() <= Constants.Model.BITS_IN_ADDRESS_MEM_CELL
                && addressRange.last.countValuableBits() <= Constants.Model.BITS_IN_ADDRESS_MEM_CELL) {
            "Possible addresses has to much bits"
        }
        check(addressRange.step == 1)
    }

    protected abstract fun read(address: Int): Int
    protected abstract fun write(address: Int, value: Int)

    final override fun tick() {
        val currentAddress = busModel.addressBus.value
        if (currentAddress !in addressRange) {
            return
        }
        when (busModel.modeBus.value) {
            BusModel.Mode.READ -> busModel.dataBus.value = read(currentAddress)
            BusModel.Mode.WRITE -> write(currentAddress, busModel.dataBus.value)
            BusModel.Mode.IDLE -> { /* do nothing */}
        }
    }
}