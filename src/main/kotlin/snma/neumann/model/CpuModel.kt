package snma.neumann.model

import java.util.*
import kotlin.random.Random

class CpuModel (
    busModel: BusModel,
) : HardwareItem(busModel) {
    private val actionsQueue: Deque<CpuAction> = LinkedList()

    enum class RegisterDescription(
        val regName: String? = null,
        val isInternal: Boolean = false,
        val type: MemoryCellModel.Type = MemoryCellModel.Type.DATA_CELL,
    ) {
        R0, R1, R2, R3, R4, R5, R6, R7, R8,

        R_STACK_POINTER(regName = "Stack Pointer", type = MemoryCellModel.Type.ADDRESS_CELL),
        R_PROGRAM_COUNTER(regName = "Program Counter", type = MemoryCellModel.Type.ADDRESS_CELL),
        R_FLAGS(regName = "Flags", type = MemoryCellModel.Type.FLAGS_CELL),

        R_A(regName = "A", isInternal = true),
        R_B(regName = "B", isInternal = true),
        R_CMD(regName = "Command", isInternal = true),
        R_ADDRESS(regName = "Address Buffer", isInternal = true, type = MemoryCellModel.Type.ADDRESS_CELL),
    }

    val registers = RegisterDescription.values().associateWith { MemoryCellModel(it.type) }

    override val memoryCells = registers.values

    override fun tick() {
        registers[RegisterDescription.R0]!!.value = registers[RegisterDescription.R0]!!.value.inv()
        if (Random.Default.nextInt(3) == 0) {
            registers[RegisterDescription.R_ADDRESS]!!.value += 1
        }
        if (Random.Default.nextInt(3) == 0) {
            registers[RegisterDescription.R_A]!!.value += 1
        }
    }
}

private sealed interface CpuAction

private enum class SimpleAction: CpuAction {
    /**
     * Wait for the next tick
     */
    TICK,

    /**
     * Address Bus := PC, Mode := Read, PC++
     */
    MEM_READ_REQUEST_BY_REG_PC,

    /**
     * Address Bus := Reg Address, Mode := Read
     */
    MEM_READ_REQUEST_BY_REG_ADDRESS,

    /**
     * Reg CMD := Data Bus, Mode := Idle
     */
    READ_CMD_FROM_DATA_BUS,

    /**
     * Reg Address := Data Bus << 8 + RegAddress & 0xFF
     */
    READ_REG_ADDRESS_HIGH_FROM_DATA_BUS,

    /**
     * Reg Address := Data Bus & 0xFF00 + Data Bus
     */
    READ_REG_ADDRESS_LOW_FROM_DATA_BUS,

    /**
     * Reg A := Data Bus
     */
    READ_REG_A_FROM_DATA_BUS,

    /**
     * Reg B := Data Bus
     */
    READ_REG_B_FROM_DATA_BUS,

    /**
     * Decide what to do after the command byte read
     */
    DECIDE_AFTER_READING_COMMAND,

    /**
     * Decide how to read the **first** argument after first byte already read.
     * Set it, if enough information is provided in the first byte.
     */
    DECIDE_CONTINUE_READ_ARG_A,

    /**
     * Decide how to read the **second** argument after first byte already read.
     * Set it, if enough information is provided in the first byte.
     */
    DECIDE_CONTINUE_READ_ARG_B,
}

private class CommandExecution(val commandCode: CommandCode) : CpuAction