// 复合文本工具（JS API）示例
// 入口：ui.compositeText(options) —— 弹窗可视化拼接，返回生成结果
//       ui.buildCompositeText(options) —— 纯拼接，不弹窗、同步返回
// 语法参考：js_documentation.md →「复合文本语法」

// 1) 弹窗：让用户自己拼，脚本拿返回值
var t = ui.compositeText({
    type: "color",
    color: "red",
    text: "限时活动",
    title: "生成一段复合文本",
});
if (t !== null) {
    console.log("用户生成：" + t);
    // 需要时复制到剪贴板（弹窗默认已复制，这里演示手动复制）
    clipboard.copy(t);
}

// 2) 纯拼接：常见形态一次生成
console.log(ui.buildCompositeText({ type: "plain", text: "普通文字" }));
console.log(ui.buildCompositeText({ type: "color", color: "gold", text: "金色" }));
console.log(ui.buildCompositeText({ type: "color", hex: "#FF0000", text: "红色" }));
console.log(ui.buildCompositeText({ type: "js", text: "vpn.isActive() ? '已断网' : '正常'" }));
console.log(ui.buildCompositeText({ type: "icon", text: "auto_collect.png" }));
console.log(ui.buildCompositeText({ type: "icon", text: "gem.png", width: "24", height: "24" }));
console.log(ui.buildCompositeText({ type: "link", url: "https://example.com", text: "点我", linkStyle: "gold" }));

// 3) 链接里放 JS 代码（点击即执行）
//    注意：显示文字里不能有冒号（解析按最后一个 : 切分）
var clickable = ui.buildCompositeText({
    type: "link",
    url: "ui.alert('提示', '你点了链接')",
    text: "点我弹提示",
});
console.log(clickable);
