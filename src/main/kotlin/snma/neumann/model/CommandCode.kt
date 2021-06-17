package snma.neumann.model

import snma.neumann.Constants

enum class CommandCode(
    val meaning: String,
    val comment: String? = null,
    val commandType: CommandType
) {
    HLT("Halt", "Stop and do nothing", CommandType.NO_ARGS),
    DLY("Delay", "Wait A ticks", CommandType.READ_1_VALUE),
    MOV("Move", "B := A", CommandType.READ_1_VALUE_AND_WRITE_TO_2ND),

    // TODO: check do we need signed and unsigned versions of operations
    ADD("Add", "B := B + A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),
    SUB("Subtract", "B := B - A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),
    MUL("Multiply", "B := B * A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),
    DIV("Divide", "B := B / A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),

    BAND("Bitwise and", "B := B & A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),
    BOR("Bitwise or", "B := B | A", CommandType.READ_2_VALUES_AND_WRITE_TO_2ND),
    BNOT("Bitwise not", "B := !A", CommandType.READ_1_VALUE_AND_WRITE_TO_2ND),

    CMP("Compare",
        "Set control bits to 0 if A = B, to -1 if A < B and to 1 otherwise",
        CommandType.READ_2_VALUES),
    JMP("Jump", "Jump to address", CommandType.JUMP_NON_CONDITIONAL),
    JEQ("Jump if equal", commandType = CommandType.JUMP_CONDITIONAL),
    JNE("Jump if not equal", commandType = CommandType.JUMP_CONDITIONAL),
    JGT("Jump if greater", commandType = CommandType.JUMP_CONDITIONAL),
    JGE("Jump if greater or equal", commandType = CommandType.JUMP_CONDITIONAL),
    JLW("Jump if lower", commandType = CommandType.JUMP_CONDITIONAL),
    JLE("Jump if lower or equal", commandType = CommandType.JUMP_CONDITIONAL),

    JSR("Jump to subroutine", "(SP--) := PC, PC := A", CommandType.JUMP_TO_SUBROUTINE),
    RET("Return from subroutine", "PC := (++SP)", CommandType.NO_ARGS),
    ;

    val stringCode get() = name

    enum class CommandType(val argsCount: Int, val description: String) {
        NO_ARGS(0, "Requires no args"),
        READ_1_VALUE(1, "Only reads one value"),
        READ_1_VALUE_AND_WRITE_TO_2ND(2, "Reads one value and writes to address of the second"),
        READ_2_VALUES(2, "Only read two values"),
        READ_2_VALUES_AND_WRITE_TO_2ND(2, "Reads two values and writes to the second"),
        JUMP_NON_CONDITIONAL(1, "Non-conditional jump to given address"),
        JUMP_CONDITIONAL(1, "Conditional jump to given address"),
        JUMP_TO_SUBROUTINE(1, "Jump to subroutine"),
        ;

        init {
            check(argsCount <= 2) { "For now max supported count of arguments is 2" }
        }
    }

    val intCode: Int
        get() = ordinal

    companion object {
        fun parse(commandWord: Int): CommandCode? {
            val ordinal = commandWord shr (2 * Constants.Model.BITS_IN_COMMAND_FOR_EACH_ADDRESSING)
            val values = values()
            return if (ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}