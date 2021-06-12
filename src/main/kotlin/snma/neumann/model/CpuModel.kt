package snma.neumann.model

import java.util.*

class CpuModel (
    busModel: BusModel,
) : HardwareItem(busModel) {
    private val actionsQueue: Deque<CpuAction> = LinkedList<CpuAction>().apply { add(SimpleAction.START_READING_COMMAND) }

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

    override fun reset() {
        super.reset()

        actionsQueue.clear()
        actionsQueue.add(SimpleAction.START_READING_COMMAND)
    }

    override fun tick() {
        // TODO: add normal logging framework
        println("CPU tick started")
        while (true) {
            val currAction = actionsQueue.pollFirst()
            println("CPU: action $currAction, tick deque: $actionsQueue")
            when (currAction) {
                null -> return
                SimpleAction.START_READING_COMMAND -> {
                    actionsQueue.addFirst(SimpleAction.START_READING_COMMAND)
                    actionsQueue.addFirst(SimpleAction.READ_CMD_FROM_DATA_BUS_AND_REQUEST_READING_ARGS)
                    actionsQueue.addFirst(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                }
                SimpleAction.TICK -> return
                SimpleAction.MEM_READ_REQUEST_BY_REG_PC -> {
                    busModel.addressBus.value = registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value
                    busModel.modeBus.value = BusModel.Mode.READ
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value += 1

                    actionsQueue.addFirst(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_ADDRESS -> TODO()
                SimpleAction.READ_CMD_FROM_DATA_BUS_AND_REQUEST_READING_ARGS -> {
                    registers[RegisterDescription.R_CMD]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE

                    val commandCode = CommandCode.getByIntCode(registers[RegisterDescription.R_CMD]!!.value)
                    if (commandCode == null) {
                        // TODO: report an error in GUI?
                        println("Unrecognized command with code ${registers[RegisterDescription.R_CMD]!!.value}")
                        return
                    }
                    actionsQueue.addFirst(CommandExecution(commandCode))
                    when (commandCode.argsCount) {
                        0 -> {}
                        1 -> {
                            actionsQueue.addFirst(SimpleAction.DECIDE_CONTINUE_READ_ARG_A)
                            actionsQueue.addFirst(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                        }
                        2 -> {
                            if (commandCode.secondArgumentReadOnlyAddress) {
                                actionsQueue.addFirst(SimpleAction.CONTINUE_READ_ARG_B_ADDRESS_ONLY)
                            } else {
                                actionsQueue.addFirst(SimpleAction.DECIDE_CONTINUE_READ_ARG_B)
                            }
                            actionsQueue.addFirst(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                            actionsQueue.addFirst(SimpleAction.DECIDE_CONTINUE_READ_ARG_A)
                            actionsQueue.addFirst(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                        }
                        else -> error("Unexpected count of args in $commandCode")
                    }
                }
                SimpleAction.READ_REG_ADDRESS_HIGH_FROM_DATA_BUS -> TODO()
                SimpleAction.READ_REG_ADDRESS_LOW_FROM_DATA_BUS -> TODO()
                SimpleAction.READ_REG_A_FROM_DATA_BUS -> TODO()
                SimpleAction.READ_REG_B_FROM_DATA_BUS -> TODO()
                SimpleAction.DECIDE_CONTINUE_READ_ARG_A -> TODO()
                SimpleAction.DECIDE_CONTINUE_READ_ARG_B -> TODO()
                SimpleAction.CONTINUE_READ_ARG_B_ADDRESS_ONLY -> TODO()
                is CommandExecution -> when (currAction.commandCode) {
                    CommandCode.HLT -> return
                    CommandCode.DLY -> TODO()
                    CommandCode.MOV -> TODO()
                    CommandCode.ADD -> TODO()
                    CommandCode.SUB -> TODO()
                    CommandCode.BAND -> TODO()
                    CommandCode.BOR -> TODO()
                    CommandCode.CMP -> TODO()
                    CommandCode.JPM -> TODO()
                    CommandCode.JEQ -> TODO()
                    CommandCode.JNE -> TODO()
                    CommandCode.JGT -> TODO()
                    CommandCode.JLW -> TODO()
                    CommandCode.JSR -> TODO()
                    CommandCode.RET -> TODO()
                }
            }
        }
    }
}

private sealed interface CpuAction

private enum class SimpleAction: CpuAction {
    /**
     * Start reading the next command
     */
    START_READING_COMMAND,

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
     *
     * Also decide how much arguments to read according to the command code
     */
    READ_CMD_FROM_DATA_BUS_AND_REQUEST_READING_ARGS,

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
     * Decide how to read the **first** argument after first byte already read.
     *
     * Set it, if enough information is provided in the first byte.
     */
    DECIDE_CONTINUE_READ_ARG_A,

    /**
     * Decide how to read the **second** argument after first byte already read.
     *
     * Set it, if enough information is provided in the first byte.
     */
    DECIDE_CONTINUE_READ_ARG_B,

    /**
     * Continue reading address of argument B
     */
    CONTINUE_READ_ARG_B_ADDRESS_ONLY,
}

private class CommandExecution(val commandCode: CommandCode) : CpuAction