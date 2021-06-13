package snma.neumann.model

import org.slf4j.LoggerFactory
import snma.neumann.CommonUtils.pushFront
import java.util.*

class CpuModel (
    busModel: BusModel,
) : HardwareItem(busModel) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO rename to stack and change the working logic accordingly
    private val actionsQueue: LinkedList<CpuAction> = LinkedList<CpuAction>().apply { add(SimpleAction.START_READING_COMMAND) }
    private var addressingModeA: AddressingMode? = null
    private var addressingModeB: AddressingMode? = null

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

    fun getOpenRegisterByIndex(index: Int): MemoryCellModel? {
        val allDescriptions = RegisterDescription.values()
        if (index !in allDescriptions.indices) {
            return null
        }
        val description = allDescriptions[index]
        if (description.isInternal) {
            return null
        }
        return registers[description]!!
    }

    override val memoryCells = registers.values

    override fun reset() {
        super.reset()

        actionsQueue.clear()
        actionsQueue.add(SimpleAction.START_READING_COMMAND)
        addressingModeA = null
        addressingModeB = null
    }

    override fun tick() {
        logger.info("Tick started")
        while (true) {
            val currAction = actionsQueue.pollFirst()
            logger.info("Action {}, actions deque: {}", currAction, actionsQueue)
            when (currAction) {
                null -> return
                SimpleAction.TICK -> return
                SimpleAction.START_READING_COMMAND -> {
                    actionsQueue.pushFront(
                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC,
                        SimpleAction.READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING,
                        SimpleAction.START_READING_COMMAND,
                    )
                }
                SimpleAction.CLEAN_BUS_MODE -> {
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_PC -> {
                    busModel.addressBus.value = registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value
                    busModel.modeBus.value = BusModel.Mode.READ
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value += 1

                    actionsQueue.addFirst(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS -> {
                    busModel.addressBus.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsQueue.addFirst(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS -> {
                    val goalRegister = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegister == null) {
                        logger.error("Wrong register number: ${busModel.dataBus.value}")
                        // TODO: notify about error in GUI
                        return
                    }
                    busModel.addressBus.value = goalRegister.value
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsQueue.addFirst(SimpleAction.TICK)
                }
                SimpleAction.READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING -> {
                    val cmdWordRead = busModel.dataBus.value
                    registers[RegisterDescription.R_CMD]!!.value = cmdWordRead

                    // Calculate command code and addressing modes
                    var commandCode = CommandCode.parse(cmdWordRead)
                    if (commandCode == null) {
                        // TODO: report an error in GUI
                        logger.error("Unrecognized command with code $cmdWordRead")
                        return
                    }
                    addressingModeA = if (commandCode.commandType.argsCount >= 1) {
                        val tmp = AddressingMode.parse(cmdWordRead, 0)
                        if (tmp == null) {
                            // TODO: report an error in GUI
                            logger.error("Unrecognized addressing mode for first argument in command $commandCode")
                            return
                        }
                        tmp
                    } else {
                        null
                    }
                    addressingModeB = if (commandCode.commandType.argsCount == 2) {
                        val tmp = AddressingMode.parse(cmdWordRead, 1)
                        if (tmp == null) {
                            // TODO: report an error in GUI
                            logger.error("Unrecognized addressing mode for second argument in command $commandCode")
                            return
                        }
                        tmp
                    } else {
                        null
                    }

                    if (commandIsConditionalJumpAndNotNeeded(commandCode)) {
                        registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value++
                        continue // go to reading next command
                    } else if (commandCode.commandType == CommandCode.CommandType.JUMP_CONDITIONAL) {
                        commandCode = CommandCode.JMP
                    }

                    actionsQueue.addFirst(CommandExecution(commandCode))
                    if (commandCode == CommandCode.DLY) { // Hack to make a delay work as expected
                        actionsQueue.addFirst(SimpleAction.INC_REG_A)
                    }
                    actionsQueue.addFirst(SimpleAction.CLEAN_BUS_MODE)

                    // Decide how to read the first argument
                    if (commandCode.commandType.argsCount >= 1) {
                        when (commandCode.commandType) {
                            CommandCode.CommandType.READ_1_VALUE,
                            CommandCode.CommandType.READ_1_VALUE_AND_WRITE_TO_2ND,
                            CommandCode.CommandType.READ_2_VALUES,
                            CommandCode.CommandType.READ_2_VALUES_AND_WRITE_TO_2ND -> {
                                when (addressingModeA ?: error("Addressing mode A should be set above")) {
                                    AddressingMode.CONSTANT -> actionsQueue.addFirst(SimpleAction.READ_REG_A_FROM_DATA_BUS)
                                    AddressingMode.REGISTER -> actionsQueue.addFirst(SimpleAction.READ_REG_A_FROM_REG_BY_DATA_BUS)
                                    AddressingMode.DIRECT -> actionsQueue.pushFront(
                                        SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS,
                                        SimpleAction.READ_REG_A_FROM_DATA_BUS
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsQueue.pushFront(
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,
                                        SimpleAction.READ_REG_A_FROM_DATA_BUS
                                    )
                                }
                            }
                            CommandCode.CommandType.JUMP_NON_CONDITIONAL,
                            CommandCode.CommandType.JUMP_TO_SUBROUTINE
                            -> {
                                when (addressingModeA ?: error("Addressing mode A should be set above")) {
                                    AddressingMode.CONSTANT -> actionsQueue.addFirst(SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS)
                                    AddressingMode.REGISTER -> actionsQueue.addFirst(SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS)
                                    AddressingMode.DIRECT -> actionsQueue.pushFront(
                                        SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS,
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsQueue.pushFront(
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS
                                    )
                                }
                            }
                            CommandCode.CommandType.JUMP_CONDITIONAL,
                            CommandCode.CommandType.NO_ARGS
                            ->
                                error("Supposed to be processed earlier")
                        }
                        actionsQueue.addFirst(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                    }

                    if (commandCode.commandType.argsCount >= 2) {
                        TODO("2nd arg is not yet supported")
                    }
                }
                SimpleAction.READ_REG_A_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_A]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_A_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegisterCell == null) {
                        logger.error("Wrong register number")
                        // TODO: notify about error in gui?
                        return
                    }
                    registers[RegisterDescription.R_A]!!.value = goalRegisterCell.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_ADDRESS]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegisterCell == null) {
                        logger.error("Wrong register number")
                        // TODO: notify about error in gui?
                        return
                    }
                    registers[RegisterDescription.R_ADDRESS]!!.value = goalRegisterCell.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.INC_REG_A -> {
                    registers[RegisterDescription.R_A]!!.value++
                }
                is CommandExecution -> {
                    check(currAction.commandCode.commandType != CommandCode.CommandType.JUMP_CONDITIONAL) {
                        "Conditional jumps should be processed on command code just read"
                    }
                    when (currAction.commandCode) {
                        CommandCode.HLT -> return
                        CommandCode.DLY -> {
                            if (registers[RegisterDescription.R_A]!!.value != 0) {
                                registers[RegisterDescription.R_A]!!.value--
                                actionsQueue.addFirst(CommandExecution(CommandCode.DLY))
                                actionsQueue.addFirst(SimpleAction.TICK)
                            }
                        }
                        CommandCode.JMP -> {
                            registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value =
                                registers[RegisterDescription.R_ADDRESS]!!.value
                        }
                        else -> TODO("Command ${currAction.commandCode} is not yet implemented")
                    }
                }
                else -> TODO("Action $currAction is not yet implemented")
            }
        }
    }

    private fun commandIsConditionalJumpAndNotNeeded(commandCode: CommandCode): Boolean {
        if (commandCode.commandType != CommandCode.CommandType.JUMP_CONDITIONAL) {
            return false
        }
        TODO()
    }
}

private sealed interface CpuAction

private enum class SimpleAction: CpuAction {
    /**
     * Start reading the next command (set up bus and add certain commands to queue)
     *
     * Address Bus := PC, Mode := Read, PC++
     */
    START_READING_COMMAND,

    /**
     * Wait for the next tick
     */
    TICK,

    /**
     * Bus Mode := Idle
     */
    CLEAN_BUS_MODE,

    /**
     * Address Bus := PC, Mode := Read, PC++
     *
     * TICK
     */
    MEM_READ_REQUEST_BY_REG_PC,

    /**
     * Address Bus := Data bus, Mode := Read
     *
     * TICK
     */
    MEM_READ_REQUEST_BY_DATA_BUS,

    /**
     * Address Bus := Reg(Data Bus), Mode := Read
     *
     * TICK
     */
    MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,

    /**
     * Reg CMD := Data Bus, Mode := Idle
     *
     * Also decide how much arguments to read according to the command code and how they are addressed
     */
    READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING,

    /**
     * Reg Address := Data Bus
     *
     * Also clean Bus Mode
     */
    READ_REG_ADDRESS_FROM_DATA_BUS,

    /**
     * Reg A := Data Bus
     *
     * Also clean Bus Mode
     */
    READ_REG_A_FROM_DATA_BUS,

    /**
     * Reg B := Data Bus
     */
    READ_REG_B_FROM_DATA_BUS,

    /**
     * Reg A := Registers(Data Bus)
     *
     * Also clean Bus Mode
     */
    READ_REG_A_FROM_REG_BY_DATA_BUS,

    /**
     * Reg Address Buffer := Registers(Data Bus)
     *
     * Also clean Bus Mode
     */
    READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS,

    /**
     * Increase a value of Register A (specially for [CommandCode.DLY] command)
     */
    INC_REG_A,
}

private data class CommandExecution(val commandCode: CommandCode) : CpuAction