# RSB 打包问题诊断报告

## 发现的问题

### 1. 版本 4 的 `rsbInfo_Length` 未设置（已修复）

**问题描述**：
- 原始 OBB 文件使用 RSB 版本 4
- 打包代码默认 `version = 3`，没有为版本 4 设置 `rsbInfo_Length` 字段
- 版本 4 的 RSB 文件在头部有一个额外的 4 字节字段 `rsbInfo_Length`

**原始文件分析**：
```
版本: 4
headLength: 0x0028D000 (167,424 bytes)
xmlPart1_BeginOffset: 0x00000000 (无 XML 资源)
rsbInfo_Length: 应该等于 headLength (因为无 XML)
```

**修复内容**：
1. 添加了 `calculateRsbInfoLength()` 函数来计算版本 4 的 `rsbInfo_Length`
2. 在 `writeFinalRsb()` 中调用此函数来设置 `rsbInfo_Length`

### 2. 可能仍然存在的问题：ZLib 压缩差异

**问题描述**：
- Java 的 `Deflater` 不是确定性的
- 相同输入可能产生不同的压缩输出
- 这可能导致重新打包的文件大小与原始文件不同

**但这不是致命问题**：
- ZLib 压缩是**可逆的**
- 即使压缩输出不同，解压后的内容应该仍然一致
- 只要解压后的内容一致，游戏应该能够正常使用

### 3. 其他可能的问题

- **PTX 编码差异**：图片格式可能有细微差异
- **文件对齐问题**：4K 对齐可能与原始文件不同
- **字节序问题**：大端/小端处理可能有差异

## 修复后的测试步骤

1. **重新打包**：
   ```bash
   # 清理旧的打包输出
   rm -rf rsb_output
   rm -rf rsb_unpacked_repacked

   # 重新打包
   ./gradlew runRsbPack
   ```

2. **运行诊断工具**：
   ```bash
   # 比较原始文件和重新打包文件的头部信息
   java -jar RsbDiagnosticKt
   ```

3. **检查关键字段**：
   - `headLength` 是否一致
   - `rsbInfo_Length` 是否一致
   - `rsgpInfo_BeginOffset` 是否一致

4. **测试游戏兼容性**：
   - 替换原始 OBB 文件
   - 启动游戏测试

## 如何验证修复效果

1. **头部信息对比**：
   - 原始文件的 `rsbInfo_Length` = 0x0028D000
   - 修复后应该也等于 0x0028D000

2. **资源偏移对比**：
   - 所有 `offset` 字段应该与原始文件一致
   - 特别关注 `rsgpInfo_BeginOffset`

3. **文件大小对比**：
   - 如果 ZLib 压缩差异是唯一问题，文件大小可能仍然不同
   - 但这不应该影响游戏运行（只要解压后内容一致）

## 下一步建议

1. 运行诊断工具查看详细的差异报告
2. 检查压缩后的数据是否完全一致
3. 如果文件大小差异较大，可能需要进一步调查 ZLib 压缩参数
