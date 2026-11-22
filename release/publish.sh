#!/usr/bin/env bash
# 戴夫工具箱发布编排脚本
#
# 两阶段设计（满足「批准后才发布」要求）：
#   prepare —— 编译 Release、读取动态版本号、生成两个产物（不联网、不发布）
#   publish —— 仅在显式 --yes 确认后才创建 GitHub Release 并上传两个产物
#
# 用法：
#   ./release/publish.sh prepare [--notes <版本说明文件>] [--apk <已有release.apk>]
#   ./release/publish.sh publish --version <x.y.z> --notes <版本说明文件> --yes
#
# 产物（输出到 release/out/）：
#   戴夫工具箱集成器_<version>.apk
#   戴夫工具箱适配包<version>.zip

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/release/out"
TEMPLATE_DIR="$ROOT/release/adaptation"
# 项目未配置签名，assembleRelease 只产出未签名 APK；旧值 composeApp-release.apk 不存在，
# 会导致 prepare 的「现场编译」分支永远失败、被迫走 --apk 旧产物捷径。
APK_BUILT="$ROOT/composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk"

REPO="${PVZ2_RELEASE_REPO:-DreamMoonCai/Pvz2Tool}"

log() { echo "[release] $*"; }
die() { echo "[release] ERROR: $*" >&2; exit 1; }

# 从 gradle 读取当前动态版本号（月.日.迭代）
get_version() {
    (cd "$ROOT" && ./gradlew :composeApp:printVersion --console=plain -q 2>/dev/null) \
        | grep -oE 'versionName=[0-9.]+' | head -1 | cut -d= -f2
}

cmd_prepare() {
    local notes="" apk_override=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --notes) notes="${2:?--notes 需要文件参数}"; shift 2 ;;
            --apk) apk_override="${2:?--apk 需要文件参数}"; shift 2 ;;
            *) die "未知参数: $1" ;;
        esac
    done

    local version
    version="$(get_version)"
    [[ -n "$version" ]] || die "无法读取版本号"
    log "版本: $version"

    mkdir -p "$OUT_DIR"

    local apk="$APK_BUILT"
    if [[ -n "$apk_override" ]]; then
        apk="$apk_override"
        log "使用指定 APK: $apk"
    else
        log "编译 Release APK ..."
        (cd "$ROOT" && ./gradlew :composeApp:assembleRelease --console=plain)
    fi
    [[ -f "$apk" ]] || die "Release APK 不存在: $apk"

    local apk_out="$OUT_DIR/戴夫工具箱集成器_${version}.apk"
    cp "$apk" "$apk_out"
    log "产物1: $apk_out"

    local notes_arg="-"
    [[ -n "$notes" && -f "$notes" ]] && notes_arg="$notes"

    local zip_out="$OUT_DIR/戴夫工具箱适配包${version}.zip"
    log "生成适配包（含从 APK 自动导出的 arsc.zip）..."
    (cd "$ROOT" && ./gradlew :composeApp:runReleaseCli --console=plain -q --args="adapt $apk $version $notes_arg $TEMPLATE_DIR $zip_out")

    [[ -f "$zip_out" ]] || die "适配包生成失败: $zip_out"
    log "产物2: $zip_out"

    # 写一份发布元数据，publish 阶段读取
    cat > "$OUT_DIR/release_meta.env" <<EOF
VERSION="$version"
APK_OUT="$apk_out"
ZIP_OUT="$zip_out"
NOTES="${notes:-}"
REPO="$REPO"
EOF
    log "准备完成。检查产物无误后执行："
    log "  ./release/publish.sh publish --yes"
}

cmd_publish() {
    local yes=0 version="" notes=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --yes) yes=1; shift ;;
            --version) version="${2:?}"; shift 2 ;;
            --notes) notes="${2:?}"; shift 2 ;;
            *) die "未知参数: $1" ;;
        esac
    done

    local meta="$OUT_DIR/release_meta.env"
    [[ -f "$meta" ]] || die "未找到发布元数据，请先执行 prepare"
    # shellcheck disable=SC1090
    source "$meta"

    version="${version:-$VERSION}"
    local apk_out="$APK_OUT" zip_out="$ZIP_OUT"
    [[ -f "$apk_out" ]] || die "产物1 不存在: $apk_out"
    [[ -f "$zip_out" ]] || die "产物2 不存在: $zip_out"

    if [[ "$yes" -ne 1 ]]; then
        log "【批准确认】即将发布以下产物到 ${REPO}:"
        log "  $apk_out"
        log "  $zip_out"
        log "确认无误请加 --yes 参数重新执行。"
        return 0
    fi

    command -v gh >/dev/null || die "未安装 gh CLI"

    local notes_file="${notes:-$NOTES}"
    local body="戴夫工具箱 $version"
    if [[ -n "$notes_file" && -f "$notes_file" ]]; then
        body="$(cat "$notes_file")"
    fi

    local tag="v$version"
    local title="戴夫工具箱 $version"
    # GitHub 上传端会剥离 asset 文件名中的非 ASCII 字符（中文被吃光）。
    # 官方方案：底层用 ASCII 文件名，把中文原名放进 label，Release 页面显示 label。
    local apk_ascii="$OUT_DIR/Pvz2Tool-Installer-${version}.apk"
    local zip_ascii="$OUT_DIR/Pvz2Tool-AdaptationPack-${version}.zip"
    cp "$apk_out" "$apk_ascii"
    cp "$zip_out" "$zip_ascii"
    local apk_label="$(basename "$apk_out")"
    local zip_label="$(basename "$zip_out")"
    log "创建 GitHub Release $tag ..."
    gh release create "$tag" \
        --repo "$REPO" \
        --title "$title" \
        --notes "$body" \
        "${apk_ascii}#${apk_label}" "${zip_ascii}#${zip_label}"
    log "发布完成: https://github.com/$REPO/releases/tag/$tag"
}

case "${1:-}" in
    prepare) shift; cmd_prepare "$@" ;;
    publish) shift; cmd_publish "$@" ;;
    *) echo "用法: $0 {prepare|publish} [options]" >&2; exit 1 ;;
esac
