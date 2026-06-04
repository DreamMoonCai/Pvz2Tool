package io.github.dreammooncai.pvz2tool.pop.plugin.io

import java.lang.Runtime

private val runtime = Runtime.getRuntime()

// 128MB 单文件自动落盘阈值
private const val MEMORY_TO_FILE_THRESHOLD = 128L * 1024L * 1024L

// 缓存 JVM/ART 最大可用堆内存 (进程生命周期内恒定)
private val maxHeap: Long = runtime.maxMemory()

// 安全边际系数 (保留 15% 堆内存作为缓冲)
private const val SAFETY_MARGIN_FACTOR = 0.15

// 缓存安全边际绝对值 (基于 maxHeap 计算，进程生命周期内恒定)
private val safetyMargin: Long = (maxHeap * SAFETY_MARGIN_FACTOR).toLong()

/**
 * 全局内存状态监控器 (优化版：提取常量)
 */
object MemoryLifecycleManager {

    /**
     * 判断当前是否处于内存高压状态
     * @param requested 此次扩容申请的字节数
     */
    fun isUnderPressure(requested: Long): Boolean {
        // 1. 快速判断：单个请求过大，直接落盘
        if (requested > MEMORY_TO_FILE_THRESHOLD) return true

        val totalHeap = runtime.totalMemory()
        val freeHeap = runtime.freeMemory()

        // 2. 计算当前真实剩余内存
        // (maxHeap - totalHeap) 是尚未向系统申请的潜力
        // freeHeap 是已申请但未使用的空间
        val reallyFree = (maxHeap - totalHeap) + freeHeap

        // 3. 判定：如果申请后剩余内存小于安全红线，视为高压
        return (reallyFree - requested) < safetyMargin
    }
}