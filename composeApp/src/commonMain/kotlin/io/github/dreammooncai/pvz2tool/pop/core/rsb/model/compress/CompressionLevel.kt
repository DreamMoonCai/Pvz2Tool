package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

// 压缩级别枚举（对应 C# CompressionLevel）
enum class CompressionLevel(val value: Int) {
    Fastest(0),
    Optimal(1),
    Smallest(3)
}