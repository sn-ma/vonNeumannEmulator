package snma.neumann.model

enum class CommandCode(
    val meaning: String,
    val argsCount: Int,
    val comment: String? = null,
    val lastArgumentTypeIfAny: LastArgumentType = LastArgumentType.REGULAR,
) {
    HLT("Halt", 0, "Stop and do nothing"),
    DLY("Delay", 1, "Wait A ticks"),
    MOV("Move", 2, "B := A", LastArgumentType.ADDRESS_TO_WRITE_AT),

    ADD("Add", 2, "B := B + A"),
    SUB("Subtract", 2, "B := B - A"),

    BAND("Bitwise and", 2, "B := B & A"),
    BOR("Bitwise or", 2, "B := B | A"),

    CMP("Compare", 2, "Set control bits to 0 if A = B, to -1 if A < B and to 1 otherwise"),
    JPM("Jump", 1, "Jump to address", LastArgumentType.ADDRESS_TO_JUMP_TO),
    JEQ("Jump if equal", 1, lastArgumentTypeIfAny = LastArgumentType.ADDRESS_TO_JUMP_TO),
    JNE("Jump if not equal", 1, lastArgumentTypeIfAny = LastArgumentType.ADDRESS_TO_JUMP_TO),
    JGT("Jump if greater", 1, lastArgumentTypeIfAny = LastArgumentType.ADDRESS_TO_JUMP_TO),
    JLW("Jump if lower", 1, lastArgumentTypeIfAny = LastArgumentType.ADDRESS_TO_JUMP_TO),

    JSR("Jump to subroutine", 1),
    RET("Return from subroutine", 0),
    ;

    val code get() = name

    enum class LastArgumentType {
        REGULAR,
        ADDRESS_TO_WRITE_AT,
        ADDRESS_TO_JUMP_TO,
    }

    val intCode: Int
        get() = ordinal

    companion object {
        fun getByIntCode(ordinal: Int): CommandCode? {
            val values = values()
            return if (ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}