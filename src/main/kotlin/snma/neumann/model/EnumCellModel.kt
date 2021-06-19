package snma.neumann.model

class EnumCellModel<T: Enum<T>>(
    defaultValue: T,
) : AbstractCellModel<T>(defaultValue, null)