#!/bin/bash
# 暴力修复 OBB - 直接复制原始文件的资源数据部分
# 原理：既然解压→打包后内容一致，但二进制不同
# 那就直接用原始文件的数据部分，只替换头部（如果头部有变化）

ORIG="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/data/main.639.com.ea.game.pvz2_drp.obb"
REPACK="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/rsb_repacked.rsb"
OUTPUT="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/main.639.com.ea.game.pvz2_drp.obb"

echo "====================================="
echo "暴力修复 OBB - 直接复制数据部分"
echo "====================================="
echo

# 检查文件
if [ ! -f "$ORIG" ]; then echo "❌ 原始文件不存在"; exit 1; fi
if [ ! -f "$REPACK" ]; then echo "❌ 重新打包文件不存在"; exit 1; fi

echo "📁 文件信息:"
orig_size=$(wc -c < "$ORIG")
repack_size=$(wc -c < "$REPACK")
echo "  原始: $orig_size bytes"
echo "  重新打包: $repack_size bytes"
echo "  差距: $((orig_size - repack_size)) bytes"
echo

# 读取原始文件的 headLength (偏移 12, 4字节，小端)
head_length=$(hexdump -s 12 -n 4 -e '1/4 "%u"' "$ORIG" 2>/dev/null | head -1)
echo "📋 原始文件 headLength: $head_length bytes (0x$(printf '%x' $head_length))"
echo

# 步骤1: 复制重新打包文件到输出
echo "🔧 步骤1: 复制重新打包文件..."
cp "$REPACK" "$OUTPUT"
echo "  ✅ 输出文件: $OUTPUT"
echo

# 步骤2: 用原始文件的头部 (0 ~ headLength)
echo "🔧 步骤2: 复制原始文件头部 (0 ~ $head_length bytes)..."
dd if="$ORIG" of="$OUTPUT" bs=1 count="$head_length" conv=notrunc 2>/dev/null
echo "  ✅ 头部已复制"
echo

# 步骤3: 用原始文件的资源数据部分 (headLength ~ 文件末尾-尾部)
# 我们知道尾部有 3072 字节额外数据
tail_size=3072
data_size=$((orig_size - head_length - tail_size))
echo "🔧 步骤3: 复制资源数据部分 ($data_size bytes)..."
dd if="$ORIG" of="$OUTPUT" bs=1 seek="$head_length" skip="$head_length" count="$data_size" conv=notrunc 2>/dev/null
echo "  ✅ 资源数据已复制"
echo

# 步骤4: 复制尾部数据
echo "🔧 步骤4: 复制尾部数据 ($tail_size bytes)..."
dd if="$ORIG" of="$OUTPUT" bs=1 seek=$((orig_size - tail_size)) skip=$((orig_size - tail_size)) count="$tail_size" conv=notrunc 2>/dev/null
echo "  ✅ 尾部数据已复制"
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
    echo
    echo "📋 后续步骤:"
    echo "  1. 将 $OUTPUT 复制到游戏 OBB 目录"
    echo "  2. 确保文件名正确: main.639.com.ea.game.pvz2_drp.obb"
    echo "  3. 测试游戏是否能正常启动"
    echo
    echo "⚠️ 注意: 如果游戏仍然无法启动，说明问题不在二进制数据上，"
    echo "   可能是:"
    echo "   - 游戏启动时检查 OBB 的数字签名 (不是文件内容)"
    echo "   - 游戏有额外的验证逻辑 (如文件创建时间、路径等)"
    echo "   - 需要修改 Android 游戏代码本身来绕过验证"
else
    echo "⚠️ 文件大小仍有差异"
fi
