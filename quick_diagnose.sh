#!/bin/bash
# 快速诊断 OBB 打包问题
# 对比原始 main.obb 和重新打包的文件

OBB_ORIG="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/data/main.639.com.ea.game.pvz2_drp.obb"
OBB_REPACK="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/rsb_output/rsb_repacked.rsb"

echo "====================================="
echo "OBB 打包问题快速诊断"
echo "====================================="
echo

# 检查文件是否存在
if [ ! -f "$OBB_ORIG" ]; then
    echo "❌ 原始文件不存在: $OBB_ORIG"
    exit 1
fi

if [ ! -f "$OBB_REPACK" ]; then
    echo "❌ 重新打包文件不存在: $OBB_REPACK"
    exit 1
fi

echo "📁 文件信息:"
echo "  原始文件: $(basename $OBB_ORIG)"
echo "    大小: $(wc -c < "$OBB_ORIG") bytes ($(echo "scale=2; $(wc -c < "$OBB_ORIG") / 1024 / 1024" | bc) MB)"
echo "  重新打包: $(basename $OBB_REPACK)"
echo "    大小: $(wc -c < "$OBB_REPACK") bytes ($(echo "scale=2; $(wc -c < "$OBB_REPACK") / 1024 / 1024" | bc) MB)"
echo "  大小差异: $(echo "$(wc -c < "$OBB_REPACK") - $(wc -c < "$OBB_ORIG")" | bc) bytes"
echo

# 对比头部 256 字节
echo "📋 头部 256 字节对比:"
echo "--------------------------------------"
echo "偏移    原始文件                    重新打包                    差异"
echo "--------------------------------------"

for offset in 0 16 32 48 64 80 96 112 128 144 160 176 192 208 224 240; do
    hex_orig=$(hexdump -s $offset -n 16 -C "$OBB_ORIG" 2>/dev/null | head -1 | cut -d' ' -f2-)
    hex_repack=$(hexdump -s $offset -n 16 -C "$OBB_REPACK" 2>/dev/null | head -1 | cut -d' ' -f2-)
    
    # 检查是否有差异
    diff=$(diff <(hexdump -s $offset -n 16 "$OBB_ORIG" 2>/dev/null) <(hexdump -s $offset -n 16 "$OBB_REPACK" 2>/dev/null))
    if [ -z "$diff" ]; then
        mark="  "
    else
        mark="⚠️"
    fi
    
    printf "0x%04X  %-24s  %-24s  %s\n" $offset "$hex_orig" "$hex_repack" "$mark"
done

echo
echo "🔍 关键字段分析:"
echo "--------------------------------------"

# 读取关键字段 (小端格式)
read32le() {
    local file=$1
    local offset=$2
    local val=$(hexdump -s $offset -n 4 -e '1/4 "%d"' "$file" 2>/dev/null)
    echo $val
}

# 版本
ver_orig=$(read32le "$OBB_ORIG" 4)
ver_repack=$(read32le "$OBB_REPACK" 4)
echo "  版本: 原始=$ver_orig vs 重新打包=$ver_repack"

# headLength (偏移 12)
head_orig=$(read32le "$OBB_ORIG" 12)
head_repack=$(read32le "$OBB_REPACK" 12)
echo "  headLength: 原始=$head_orig (0x$(printf '%x' $head_orig)) vs 重新打包=$head_repack (0x$(printf '%x' $head_repack))"

# fileList_Length (偏移 16)
filelist_orig=$(read32le "$OBB_ORIG" 16)
filelist_repack=$(read32le "$OBB_REPACK" 16)
echo "  fileList_Length: 原始=$filelist_orig vs 重新打包=$filelist_repack"

# rsgpInfo_BeginOffset (偏移 24)
rsgpoff_orig=$(read32le "$OBB_ORIG" 24)
rsgpoff_repack=$(read32le "$OBB_REPACK" 24)
echo "  rsgpInfo_BeginOffset: 原始=$rsgpoff_orig vs 重新打包=$rsgpoff_repack"

# rsgp_Number (偏移 32)
rsgpnum_orig=$(read32le "$OBB_ORIG" 32)
rsgpnum_repack=$(read32le "$OBB_REPACK" 32)
echo "  rsgp_Number: 原始=$rsgpnum_orig vs 重新打包=$rsgpnum_repack"

echo
echo "💡 分析建议:"
echo "  1. 如果 headLength 不同，说明头部信息长度计算有误"
echo "  2. 如果 rsgp_Number 不同，说明资源组数量不一致"
echo "  3. 如果文件大小差异巨大，可能打包了错误的内容"

echo
echo "====================================="
echo "诊断完成"
echo "====================================="
