#!/bin/bash
# 最终修复脚本 - 完全复制原始 OBB 的关键头部字段
# 问题：即使内容一致，游戏仍无法识别重新打包的 OBB
# 方案：直接复制原始文件的头部到新文件

ORIG="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/data/main.639.com.ea.game.pvz2_drp.obb"
REPACK="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/rsb_repacked.rsb"
OUTPUT="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/main.639.com.ea.game.pvz2_drp.obb"

echo "======================================"
echo "OOB 最终修复 - 复制原始头部"
echo "======================================"
echo

# 检查文件
if [ ! -f "$ORIG" ]; then
    echo "❌ 原始文件不存在: $ORIG"
    exit 1
fi

if [ ! -f "$REPACK" ]; then
    echo "❌ 重新打包文件不存在: $REPACK"
    echo "   请先运行: ./gradlew :composeApp:jvmRun"
    exit 1
fi

echo "📁 文件信息:"
orig_size=$(wc -c < "$ORIG")
repack_size=$(wc -c < "$REPACK")
echo "  原始: $orig_size bytes"
echo "  重新打包: $repack_size bytes"
echo "  差距: $((orig_size - repack_size)) bytes"
echo

# 步骤1: 复制重新打包文件到输出
echo "🔧 步骤1: 复制重新打包文件..."
cp "$REPACK" "$OUTPUT"
echo "  ✅ 输出文件: $OUTPUT"
echo

# 步骤2: 复制原始文件的前 16KB（头部）到新文件
echo "🔧 步骤2: 复制原始文件头部 (16KB)..."
dd if="$ORIG" of="$OUTPUT" bs=1 count=16384 conv=notrunc 2>/dev/null
echo "  ✅ 头部已复制 (16KB)"
echo

# 步骤3: 复制原始文件的尾部数据（如果有）
if [ "$orig_size" -gt "$repack_size" ]; then
    tail_size=$((orig_size - repack_size))
    echo "🔧 步骤3: 复制尾部数据 ($tail_size bytes)..."
    dd if="$ORIG" of="$OUTPUT" bs=1 seek="$repack_size" skip="$repack_size" count="$tail_size" 2>/dev/null
    echo "  ✅ 尾部数据已复制"
else
    echo "🔧 步骤3: 无需复制尾部数据 (文件大小一致或新文件更大)"
fi
echo

# 验证结果
new_size=$(wc -c < "$OUTPUT")
echo "📊 最终文件:"
echo "  输出: $OUTPUT"
echo "  大小: $new_size bytes"
echo "  与原始差异: $((new_size - orig_size)) bytes"
echo

if [ "$new_size" -eq "$orig_size" ]; then
    echo "✅ 文件大小现在完全一致!"
else
    echo "⚠️ 文件大小仍有差异"
fi
echo

echo "📋 后续步骤:"
echo "  1. 将 $OUTPUT 复制到游戏 OBB 目录"
echo "  2. 确保文件名正确: main.639.com.ea.game.pvz2_drp.obb"
echo "  3. 测试游戏是否能正常启动"
echo
echo "⚠️ 如果游戏仍然无法启动，可能原因:"
echo "  - 游戏有额外的签名或校验机制（如数字签名）"
echo "  - 资源数据虽然一致，但压缩方式导致解压慢或出错"
echo "  - Android 系统对 OBB 文件有特殊的验证机制"
