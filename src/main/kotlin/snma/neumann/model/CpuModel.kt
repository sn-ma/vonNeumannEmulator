package snma.neumann.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.slf4j.LoggerFactory
import snma.neumann.utils.CommonUtils
import snma.neumann.utils.rx.getValue
import snma.neumann.utils.rx.setValue
import kotlin.math.sign

class CpuModel (
    busModel: BusModel,
) : BusConnectedHardwareItem(busModel) {
    private val currCommandBehaviorSubject: BehaviorSubject<Triple<CommandCode?, AddressingMode?, AddressingMode?>> =
        BehaviorSubject.createDefault(Triple(null, null, null))
    val currCommandObservable: Observable<Triple<CommandCode?, AddressingMode?, AddressingMode?>>
        get() = currCommandBehaviorSubject
    var currCommand: Triple<CommandCode?, AddressingMode?, AddressingMode?> by currCommandBehaviorSubject
        private set

    private val logger = LoggerFactory.getLogger(javaClass)

    private val actionsStack = MyStack<CpuAction>()

    private val isStoppedBehaviorSubject = BehaviorSubject.createDefault<StopMode>(CommonStopMode.NOT_STOPPED).apply {
        distinctUntilChanged().subscribe { newValue ->
            actionsStack.clear()
            if (newValue == CommonStopMode.NOT_STOPPED) {
                actionsStack.push(SimpleAction.START_READING_COMMAND)
            }
        }
    }
    val isStoppedObservable: Observable<StopMode> get() = isStoppedBehaviorSubject
    var isStopped: StopMode by isStoppedBehaviorSubject

    sealed interface StopMode
    enum class CommonStopMode: StopMode { NOT_STOPPED, HALTED }
    data class ErrorStopMode(val description: String): StopMode

    enum class RegisterDescription(
        val regName: String? = null,
        val isInternal: Boolean = false,
        val type: MemoryCellModel.Type = MemoryCellModel.Type.DATA_CELL,
    ) {
        R0, R1, R2, R3, R4, R5, R6, R7,

        R_PROGRAM_COUNTER(regName = "Program Counter", type = MemoryCellModel.Type.ADDRESS_CELL),
        R_FLAGS(regName = "Flags", type = MemoryCellModel.Type.FLAGS_CELL),
        R_STACK_POINTER(regName = "Stack Pointer", type = MemoryCellModel.Type.ADDRESS_CELL),

        R_A(regName = "A", isInternal = true),
        R_B(regName = "B", isInternal = true),
        R_ADDRESS(regName = "Address Buffer", isInternal = true, type = MemoryCellModel.Type.ADDRESS_CELL),
    }

    val registers = RegisterDescription.values().associateWith { MemoryCellModel(it.type) }

    override val memoryCells = registers.values

    private fun getOpenRegisterByIndex(index: Int): MemoryCellModel? {
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

        actionsStack.clear()
        actionsStack.push(SimpleAction.START_READING_COMMAND)
        isStopped = CommonStopMode.NOT_STOPPED

        currCommand = Triple(null, null, null)
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
            @Suppress("REDUNDANT_ELSE_IN_WHEN") // "else" branch should be present even when "when" is exhaustive for the case of newly added commands
            when (currAction) {
                null,
                SimpleAction.TICK -> {
                    return
                }
                SimpleAction.START_READING_COMMAND -> {
                    currCommand = Triple(null, null, null)
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
                    busModel.addressBus.value = registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value
                    busModel.modeBus.value = BusModel.Mode.READ
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value += 1

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS -> {
                    busModel.addressBus.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS -> {
                    val goalRegister = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegister == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.value)
                        return
                    }
                    busModel.addressBus.value = goalRegister.value
                    busModel.modeBus.value = BusModel.Mode.READ

                    actionsStack.push(SimpleAction.TICK)
                }
                SimpleAction.READ_CMD_FROM_DATA_BUS_AND_DECIDE_ABOUT_ARGS_READING -> {
                    val cmdWordRead = busModel.dataBus.value

                    // Calculate command code and addressing modes
                    var commandCode = CommandCode.parse(cmdWordRead)
                    if (commandCode == null) {
                        setErrorInCodeWord("Unrecognized command", cmdWordRead)
                        return
                    }
                    val addressingModeA = if (commandCode.commandType.argsCount >= 1) {
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
                    val addressingModeB = if (commandCode.commandType.argsCount == 2) {
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

                    currCommand = Triple(commandCode, addressingModeA, addressingModeB)

                    if (commandIsConditionalJumpAndNotNeeded(commandCode)) {
                        registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value++
                        continue // go to reading next command
                    } else if (commandCode.commandType == CommandCode.CommandType.JUMP_CONDITIONAL) {
                        commandCode = CommandCode.JMP
                    }

                    // Command result writing (result should be in Reg B)
                    when (commandCode.commandType) {
                        CommandCode.CommandType.NO_ARGS,
                        CommandCode.CommandType.READ_1_VALUE,
                        CommandCode.CommandType.READ_2_VALUES,
                        CommandCode.CommandType.JUMP_NON_CONDITIONAL,
                        CommandCode.CommandType.JUMP_CONDITIONAL,
                        CommandCode.CommandType.JUMP_TO_SUBROUTINE -> {
                            /* No need to write any result */
                        }
                        CommandCode.CommandType.READ_1_VALUE_AND_WRITE_TO_2ND,
                        CommandCode.CommandType.READ_2_VALUES_AND_WRITE_TO_2ND -> {
                            when (addressingModeB ?: error("Addressing mode B should not be null now")) {
                                AddressingMode.REGISTER -> {
                                    actionsStack.push(SimpleAction.WRITE_RESULT_TO_REG_BY_REG_ADDRESS)
                                }
                                AddressingMode.CONSTANT,
                                AddressingMode.DIRECT,
                                AddressingMode.REGISTER_INDIRECT -> {
                                    actionsStack.push(SimpleAction.WRITE_RESULT_TO_MEMORY_BY_REG_ADDRESS)
                                }
                            }
                        }
                    }

                    // Command execution
                    actionsStack.push(CommandExecution(commandCode))
                    if (commandCode == CommandCode.DLY) { // Hack to make a delay work as expected
                        actionsStack.push(SimpleAction.INC_REG_A)
                    }
                    actionsStack.push(SimpleAction.CLEAN_BUS_MODE)

                    // Read 2nd command argument
                    if (commandCode.commandType.argsCount >= 2) {
                        when (commandCode.commandType) {
                            CommandCode.CommandType.READ_1_VALUE_AND_WRITE_TO_2ND -> {
                                when (addressingModeB ?: error("Addressing mode B should be set above")) {
                                    AddressingMode.CONSTANT -> actionsStack.push(
                                        SimpleAction.INC_PROGRAM_COUNTER,
                                        SimpleAction.SET_REG_ADDRESS_TO_PC
                                    )
                                    AddressingMode.REGISTER,
                                    AddressingMode.DIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS, SimpleAction.MEM_READ_REQUEST_BY_REG_PC
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS, SimpleAction.MEM_READ_REQUEST_BY_REG_PC
                                    )
                                }
                            }
                            CommandCode.CommandType.READ_2_VALUES,
                            CommandCode.CommandType.READ_2_VALUES_AND_WRITE_TO_2ND -> {
                                when (addressingModeB ?: error("Addressing mode B should be set above")) {
                                    AddressingMode.CONSTANT -> actionsStack.push(
                                        SimpleAction.READ_REG_B_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC,
                                        SimpleAction.SET_REG_ADDRESS_TO_PC
                                    )
                                    AddressingMode.REGISTER -> actionsStack.push(
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS,
                                        SimpleAction.READ_REG_B_FROM_REG_BY_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC
                                    )
                                    AddressingMode.DIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_B_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_DATA_BUS,
                                        SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC
                                    )
                                    AddressingMode.REGISTER_INDIRECT -> actionsStack.push(
                                        SimpleAction.READ_REG_B_FROM_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_BY_DATA_BUS,
                                        SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS,
                                        SimpleAction.MEM_READ_REQUEST_BY_REG_PC
                                    )
                                }
                            }

                            CommandCode.CommandType.NO_ARGS,
                            CommandCode.CommandType.READ_1_VALUE,
                            CommandCode.CommandType.JUMP_NON_CONDITIONAL,
                            CommandCode.CommandType.JUMP_CONDITIONAL,
                            CommandCode.CommandType.JUMP_TO_SUBROUTINE -> {
                                error("Command type ${commandCode.commandType} should have less than 2 args")
                            }
                        }
                    }

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
                            CommandCode.CommandType.JUMP_TO_SUBROUTINE -> {
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
                }
                SimpleAction.READ_REG_A_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_A]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_A_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.value)
                        return
                    }
                    registers[RegisterDescription.R_A]!!.value = goalRegisterCell.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.READ_REG_ADDRESS_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_ADDRESS]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.SET_REG_ADDRESS_TO_PC -> {
                    registers[RegisterDescription.R_ADDRESS]!!.value =
                        registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value
                }
                SimpleAction.INC_PROGRAM_COUNTER -> {
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value++
                }
                SimpleAction.READ_REG_ADDRESS_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.value)
                        return
                    }
                    registers[RegisterDescription.R_ADDRESS]!!.value = goalRegisterCell.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
                }
                SimpleAction.INC_REG_A -> {
                    registers[RegisterDescription.R_A]!!.value++
                }
                SimpleAction.READ_REG_B_FROM_DATA_BUS -> {
                    registers[RegisterDescription.R_B]!!.value = busModel.dataBus.value
                }
                SimpleAction.READ_REG_B_FROM_REG_BY_DATA_BUS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(busModel.dataBus.value)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", busModel.dataBus.value)
                        return
                    }
                    registers[RegisterDescription.R_B]!!.value = goalRegisterCell.value
                }
                SimpleAction.WRITE_RESULT_TO_REG_BY_REG_ADDRESS -> {
                    val goalRegisterCell = getOpenRegisterByIndex(registers[RegisterDescription.R_ADDRESS]!!.value)
                    if (goalRegisterCell == null) {
                        setErrorInCodeWord("Wrong register number", registers[RegisterDescription.R_ADDRESS]!!.value)
                        return
                    }

                    goalRegisterCell.value = registers[RegisterDescription.R_B]!!.value
                }
                SimpleAction.WRITE_RESULT_TO_MEMORY_BY_REG_ADDRESS -> {
                    busModel.addressBus.value = registers[RegisterDescription.R_ADDRESS]!!.value
                    busModel.dataBus.value = registers[RegisterDescription.R_B]!!.value
                    busModel.modeBus.value = BusModel.Mode.WRITE

                    actionsStack.push(SimpleAction.CLEAN_BUS_MODE, SimpleAction.TICK)
                }
                SimpleAction.FINISH_RET -> {
                    registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value = busModel.dataBus.value
                    busModel.modeBus.value = BusModel.Mode.IDLE
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
                            if (registers[RegisterDescription.R_A]!!.value != 0) {
                                registers[RegisterDescription.R_A]!!.value--
                                actionsStack.push(CommandExecution(CommandCode.DLY))
                                actionsStack.push(SimpleAction.TICK)
                            }
                        }
                        CommandCode.MOV -> {
                            registers[RegisterDescription.R_B]!!.value =
                                registers[RegisterDescription.R_A]!!.value
                        }

                        CommandCode.ADD -> {
                            registers[RegisterDescription.R_B]!!.value +=
                                registers[RegisterDescription.R_A]!!.value
                        }
                        CommandCode.SUB -> {
                            registers[RegisterDescription.R_B]!!.value -=
                                registers[RegisterDescription.R_A]!!.value
                        }
                        CommandCode.MUL -> {
                            registers[RegisterDescription.R_B]!!.value *=
                                registers[RegisterDescription.R_A]!!.value
                        }
                        CommandCode.DIV -> {
                            val denominator = registers[RegisterDescription.R_A]!!.value
                            if (denominator == 0) {
                                setError("Zero division")
                                return
                            }
                            registers[RegisterDescription.R_B]!!.value /= denominator
                        }
                        CommandCode.BAND -> {
                            registers[RegisterDescription.R_B]!!.value =
                                registers[RegisterDescription.R_B]!!.value and registers[RegisterDescription.R_A]!!.value
                        }
                        CommandCode.BOR -> {
                            registers[RegisterDescription.R_B]!!.value =
                                registers[RegisterDescription.R_B]!!.value or registers[RegisterDescription.R_A]!!.value
                        }
                        CommandCode.BNOT -> {
                            registers[RegisterDescription.R_B]!!.value =
                                registers[RegisterDescription.R_A]!!.value.inv()
                        }

                        CommandCode.CMP -> {
                            registers[RegisterDescription.R_FLAGS]!!.value =
                                (registers[RegisterDescription.R_A]!!.value -
                                        registers[RegisterDescription.R_B]!!.value).sign
                        }

                        CommandCode.JMP -> {
                            registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value =
                                registers[RegisterDescription.R_ADDRESS]!!.value
                        }

                        CommandCode.JSR -> {
                            busModel.addressBus.value = registers[RegisterDescription.R_STACK_POINTER]!!.value
                            busModel.dataBus.value = registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value
                            busModel.modeBus.value = BusModel.Mode.WRITE
                            registers[RegisterDescription.R_PROGRAM_COUNTER]!!.value =
                                registers[RegisterDescription.R_ADDRESS]!!.value
                            registers[RegisterDescription.R_STACK_POINTER]!!.value--
                            actionsStack.push(SimpleAction.CLEAN_BUS_MODE, SimpleAction.TICK)
                        }

                        CommandCode.RET -> {
                            registers[RegisterDescription.R_STACK_POINTER]!!.value++
                            busModel.addressBus.value = registers[RegisterDescription.R_STACK_POINTER]!!.value
                            busModel.modeBus.value = BusModel.Mode.READ

                            actionsStack.push(SimpleAction.FINISH_RET, SimpleAction.TICK)
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
        val cmpResult = when(registers[RegisterDescription.R_FLAGS]!!.value) {
            1 -> 1
            0 -> 0
            3 -> -1 // because of bit clipping
            else -> error("Unexpected value of compare register (${registers[RegisterDescription.R_FLAGS]!!.value})")
        }
        return when (commandCode) {
            CommandCode.JEQ -> cmpResult != 0
            CommandCode.JNE -> cmpResult == 0
            CommandCode.JGT -> cmpResult <= 0
            CommandCode.JGE -> cmpResult < 0
            CommandCode.JLW -> cmpResult >= 0
            CommandCode.JLE -> cmpResult > 0
            else -> error("$commandCode is unexpected here")
        }
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
     * Reg Address := Program Counter
     */
    SET_REG_ADDRESS_TO_PC,

    /**
     * Program Counter += 1
     */
    INC_PROGRAM_COUNTER,

    /**
     * Reg A := Data Bus
     *
     * Also clean Bus Mode
     */
    READ_REG_A_FROM_DATA_BUS,

    /**
     * Reg A := Registers(Data Bus)
     *
     * Also clean Bus Mode
     */
    READ_REG_A_FROM_REG_BY_DATA_BUS,

    /**
     * Reg B := Data Bus
     */
    READ_REG_B_FROM_DATA_BUS,

    /**
     * Reg B := Reg(Data Bus)
     */
    READ_REG_B_FROM_REG_BY_DATA_BUS,

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

    /**
     * Reg(Reg Address) := Reg B
     */
    WRITE_RESULT_TO_REG_BY_REG_ADDRESS,

    /**
     * Address Bus := Reg Address, Data Bus := Reg B, Mode := Write
     *
     * TICK
     */
    WRITE_RESULT_TO_MEMORY_BY_REG_ADDRESS,

    /**
     * Stack Pointer := Data Bus
     *
     * Also clean Bus Mode
     */
    FINISH_RET,
}

private data class CommandExecution(val commandCode: CommandCode) : CpuAction