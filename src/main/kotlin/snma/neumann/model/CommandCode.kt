package snma.neumann.model

enum class CommandCode(val meaning: String, val argsCount: Int, val comment: String? = null) {
    HLT("Halt", 0, "Stop and do nothing"),
    DLY("Delay", 1, "Wait A ticks"),
    MOV("Move", 2, "B := A"),

    ADD("Add", 2, "B := B + A"),
    SUB("Subtract", 2, "B := B - A"),

    BAND("Bitwise and", 2, "B := B & A"),
    BOR("Bitwise or", 2, "B := B | A"),

    CMP("Compare", 2, "Set control bits to 0 if A = B, to -1 if A < B and to 1 otherwise"),
    JPM("Jump", 1, "Jump to address"),
    JEQ("Jump if equal", 1),
    JNE("Jump if not equal", 1),
    JGT("Jump if greater", 1),
    JLW("Jump if lower", 1),

    JSR("Jump to subroutine", 1),
    RET("Return from subroutine", 0),
}