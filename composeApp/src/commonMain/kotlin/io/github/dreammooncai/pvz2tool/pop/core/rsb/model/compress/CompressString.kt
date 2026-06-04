package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

data class CompressString(
    var name: String?,
    var extraInfo: ExtraInfo?,
    var type: Int
) {
    val fileName: String? get() = name?.replace("\\","/")

    constructor(): this(null, null, -1)

    constructor(name: String?, extraInfo: ExtraInfo) : this(name,extraInfo,when (extraInfo) {
        is RsbExtraInfo -> 0
        is RsgpPart0ExtraInfo -> 1
        is RsgpPart1ExtraInfo -> 2
        else -> -1
    })
}