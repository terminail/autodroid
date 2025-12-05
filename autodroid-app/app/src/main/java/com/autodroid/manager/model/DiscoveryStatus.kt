package com.autodroid.manager.model

/**
 * 封装mDNS发现状态信息
 */
data class DiscoveryStatus(
    val inProgress: Boolean = false,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val failed: Boolean = false
) {
    /**
     * 检查是否正在进行发现
     */
    fun isDiscovering(): Boolean = inProgress
    
    /**
     * 检查是否已达到最大重试次数
     */
    fun hasReachedMaxRetries(): Boolean = retryCount >= maxRetries
    
    /**
     * 检查发现是否失败（包括达到最大重试次数）
     */
    fun isDiscoveryFailed(): Boolean = failed || hasReachedMaxRetries()
    
    /**
     * 创建一个新的发现状态，增加重试次数
     */
    fun withIncrementedRetry(): DiscoveryStatus = copy(
        retryCount = retryCount + 1
    )
    
    /**
     * 开始发现的初始状态
     */
    companion object {
        fun initial(maxRetries: Int = 3): DiscoveryStatus = DiscoveryStatus(
            inProgress = false,
            retryCount = 0,
            maxRetries = maxRetries,
            failed = false
        )
        
        /**
         * 开始发现的状态
         */
        fun discovering(maxRetries: Int = 3): DiscoveryStatus = DiscoveryStatus(
            inProgress = true,
            retryCount = 0,
            maxRetries = maxRetries,
            failed = false
        )
        
        /**
         * 发现失败的状态
         */
        fun failed(maxRetries: Int = 3, retryCount: Int = 0): DiscoveryStatus = DiscoveryStatus(
            inProgress = false,
            retryCount = retryCount,
            maxRetries = maxRetries,
            failed = true
        )
    }
}