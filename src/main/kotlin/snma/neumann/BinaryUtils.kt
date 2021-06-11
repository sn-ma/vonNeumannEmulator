package snma.neumann

object BinaryUtils {
    fun Int.countValuableBits(): Int = if (this == 0) 0 else toString(2).length
}