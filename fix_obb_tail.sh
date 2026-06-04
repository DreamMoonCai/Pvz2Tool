#!/bin/bash
# OBB 打包修复脚本 - 直接复制尾部数据
# 问题：重新打包的 OBB 丢失了尾部 3072 字节数据

OBB_ORIG="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/data/main.639.com.ea.game.pvz2_drp.obb"
OBB_REPACK="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/rsb_repacked.rsb"
OBB_FIXED="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/main.639.com.ea.game.pvz2_drp.obb"

echo "======================================"
echo "OBB 打包修复 - 尾部数据恢复"
echo "======================================"
echo

# 检查文件
if [ ! -f "$OBB_ORIG" ]; then
    echo "❌ 原始文件不存在: $OBB_ORIG"
    exit 1
fi

if [ ! -f "$OBB_REPACK" ]; then
    echo "❌ 重新打包文件不存在: $OBB_REPACK"
    echo "   请先运行打包测试"
    exit 1
fi

# 获取文件大小
orig_size=$(wc -c < "$OBB_ORIG")
repack_size=$(wc -c < "$OBB_REPACK")

echo "📁 文件信息:"
echo "  原始: $orig_size bytes"
echo "  重新打包: $repack_size bytes"
echo "  差异: $((orig_size - repack_size)) bytes"
echo

# 检查是否需要修复
if [ "$orig_size" -le "$repack_size" ]; then
    echo "✅ 文件大小一致或重新打包更大，无需修复"
    exit 0
fi

# 需要修复：从原始文件复制尾部数据
tail_size=$((orig_size - repack_size))
echo "🔧 需要修复: 复制尾部 $tail_size 字节..."

# 复制重新打包文件到输出
cp "$OBB_REPACK" "$OBB_FIXED"

# 用 dd 复制尾部数据
echo "  使用 dd 复制尾部数据..."
dd if="$OBB_ORIG" of="$OBB_FIXED" bs=1 seek="$repack_size" skip="$repack_size" count="$tail_size" 2>/dev/null

# 验证结果
new_size=$(wc -c < "$OBB_FIXED")
echo "✅ 修复完成!"
echo "  输出文件: $OBB_FIXED"
echo "  新大小: $new_size bytes"
echo "  与原始差异: $((new_size - orig_size)) bytes"

if [ "$new_size" -eq "$orig_size" ]; then
    echo "  ✅ 文件大小现在完全一致!"
else
    echo "  ⚠️ 文件大小仍有差异"
fi

echo
echo "📋 后续步骤:"
echo "  1. 将 $OBB_FIXED 复制到游戏 OBB 目录"
echo "  2. 确保文件名正确: main.639.com.ea.game.pvz2_drp.obb"
echo "  3. 测试游戏是否能正常启动"
echo
echo "⚠️ 注意: 如果游戏仍然无法启动，可能原因:"
echo "  - 游戏有额外的签名或校验机制"
echo "  - 压缩参数的细微差异"
echo "  - 资源组顺序或偏移量不一致"
