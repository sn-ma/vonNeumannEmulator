package snma.neumann.model

import javafx.beans.property.SimpleObjectProperty
import org.slf4j.LoggerFactory
import snma.neumann.CommonUtils
import tornadofx.getValue
import tornadofx.setValue

class CpuModel (
    busModel: BusModel,
) : BusConnectedHardwareItem(busModel) {
    val isStoppedProperty = object : SimpleObjectProperty<StopMode>(CommonStopMode.NOT_STOPPED) {
        override fun set(newValue: StopMode) {
            if (value != newValue) {
                actionsStack.clear()
                if (newValue == CommonStopMode.NOT_STOPPED) {
                    actionsStack.push(SimpleAction.START_READING_COMMAND)
                }
            }
            super.set(newValue)
        }
    }
    var isStopped: StopMode by isStoppedProperty

    sealed interface StopMode
    enum class CommonStopMode: StopMode { NOT_STOPPED, HALTED }
    data class ErrorStopMode(val description: String): StopMode

    private val logger = LoggerFactory.getLogger(javaClass)

    private val actionsStack = MyStack<CpuAction>().apply { push(SimpleAction.START_READING_COMMAND) }
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

    override val memoryCells = registers.values

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

    override fun reset() {
        super.reset()

        addressingModeA = null
        addressingModeB = null
        isStopped = CommonStopMode.NOT_STOPPED
    }

    private fun setError(description: String) {
        val message = "Error: $description"
        logger.error(message)
        isStopped = ErrorStopMode(message)
    }

    private fun setErrorInCodeWord(description: String, codeWord: Int) {
        val commandWordString = CommonUtils.intToHexString(codeWord, 2)
        setError("$description (code $commandWordString")
    }

    override fun tick() {
        logger.info("Tick started")
        while (true) {
            val currAction = actionsStack.poll()
            logger.info("Action {}, actions stack: {}", currAction, actionsStack)
            when (currAction) {
                null -> return
                SimpleAction.TICK -> return
                SimpleAction.START_READING_COMMAND -> {
                    actionsStack.push(
                        SimpleAction.START_READING_COMMAND,
                        SimpleAction.READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING,
                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC,
                    )
                }
                SimpleAction.CLEAN_BUS_MODE -> {
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_PC -> {
                    busModel.addressBus.intValue = registers[RegisterDescription.R_PROGRAM_COUNTER]!!.intValue
                    busModel.modeBus.value = BusModel.Mode.READ
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.intValue += 1

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS -> {
                    busModel.addressBus.intValue = busModel.dataBus.intValue
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS -> {
                    val goalRegister = getOpenRegisterByIndex(busModel.dataBus.intValue)
                    if (goalRegister == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.intValue)
                        return
                    }
                    busModel.addressBus.intValue = goalRegister.intValue
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING -> {
                    val cmdWordRead = busModel.dataBus.intValue
                    registers[RegisterDescription.R_CMD]!!.intValue = cmdWordRead

                    // Calculate command code and addressing modes
                    var commandCode = CommandCode.parse(cmdWordRead)
                    if (commandCode == null) {
                        setErrorInCodeWord("Unrecognized command", cmdWordRead)
                        return
                    }
                    addressingModeA = if (commandCode.commandType.argsCount >= 1) {
                        val tmp = AddressingMode.parse(cmdWordRead, 0)
                        if (tmp == null) {
                            setErrorInCodeWord(
                                "Unrecognized addressing mode for first argument in the given command", cmdWordRead)
                            return
                        }
                        tmp
                    } else {
                        null
                    }
                    addressingModeB = if (commandCode.commandType.argsCount == 2) {
                        val tmp = AddressingMode.parse(cmdWordRead, 1)
                        if (tmp == null) {
                            setErrorInCodeWord(
                                "Unrecognized addressing mode for second argument in the given command", cmdWordRead)
                            return
                        }
                        tmp
                    } else {
                        null
                    }

                    if (commandIsConditionalJumpAndNotNeeded(commandCode)) {
                        registers[RegisterDescription.R_PROGRAM_COUNTER]!!.intValue++
                        continue // go to reading next command
                    } else if (commandCode.commandType == CommandCode.CommandType.JUMP_CONDITIONAL) {
                        commandCode = CommandCode.JMP
                    }

                    actionsStack.push(CommandExecution(commandCode))
                    if (commandCode == CommandCode.DLY) { // Hack to make a delay work as expected
                        actionsStack.push(SimpleAction.INC_REG_A)
                    }
                    actionsStack.push(SimpleAction.CLEAN_BUS_MODE)

                    // Decide how to read the first argument
                    if (commandCode.commandType.argsCount >= 1) {
                        when (commandCode.commandType) {
                            CommandCode.CommandType.READ_1_VALUE,
                            CommandCode.CommandType.READ_1_VALUE_AND_WRITE_TO_2ND,
                            CommandCode.CommandType.READ_2_VALUES,
                            CommandCode.CommandType.READ_2_VALUES_AND_WRITE_TO_2ND -> {
                                when (addressingModeA ?: error("Addressing mode A should be set above")) {
                                    AddressingMode.CONSTANT -> actionsStack.push(SimpleAction.READ_REG_A_FROM_DATA_BUS)
                                    AddressingMode.REGISTER -> actionsStack.push(SimpleAction.READ_REG_A_FROM_REG_BY_DATA_BUS)
                                    AddressingMode.DIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_A_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS,
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_A_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,
                                    )
                                }
                            }
                            CommandCode.CommandType.JUMP_NON_CONDITIONAL,
                            CommandCode.CommandType.JUMP_TO_SUBROUTINE
                            -> {
                                when (addressingModeA ?: error("Addressing mode A should be set above")) {
                                    AddressingMode.CONSTANT -> actionsStack.push(SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS)
                                    AddressingMode.REGISTER -> actionsStack.push(SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS)
                                    AddressingMode.DIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS,
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,
                                    )
                                }
                            }
                            CommandCode.CommandType.JUMP_CONDITIONAL,
                            CommandCode.CommandType.NO_ARGS -> {
                                error("Supposed to be processed earlier")
                            }
                        }
                        actionsStack.push(SimpleAction.MEM_READ_REQUEST_BY_REG_PC)
                    }

                    if (commandCode.commandType.argsCount >= 2) {
                        TODO("2nd arg is not yet supported")
                    }
                }
                SimpleAction.READ_REG_A_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_A]!!.intValue = busModel.dataBus.intValue
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_A_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.intValue)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.intValue)
                        return
                    }
                    registers[RegisterDescription.R_A]!!.intValue = goalRegisterCell.intValue
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_ADDRESS]!!.intValue = busModel.dataBus.intValue
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.intValue)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.intValue)
                        return
                    }
                    registers[RegisterDescription.R_ADDRESS]!!.intValue = goalRegisterCell.intValue
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.INC_REG_A -> {
                    registers[RegisterDescription.R_A]!!.intValue++
                }
                is CommandExecution -> {
                    check(currAction.commandCode.commandType != CommandCode.CommandType.JUMP_CONDITIONAL) {
                        "Conditional jumps should be processed on command code just read"
                    }
                    when (currAction.commandCode) {
                        CommandCode.HLT -> {
                            isStopped = CommonStopMode.HALTED
                            return
                        }
                        CommandCode.DLY -> {
                            if (registers[RegisterDescription.R_A]!!.intValue != 0) {
                                registers[RegisterDescription.R_A]!!.intValue--
                                actionsStack.push(CommandExecution(CommandCode.DLY))
                                actionsStack.push(SimpleAction.TICK)
                            }
                        }
                        CommandCode.JMP -> {
                            registers[RegisterDescription.R_PROGRAM_COUNTER]!!.intValue =
                                registers[RegisterDescription.R_ADDRESS]!!.intValue
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