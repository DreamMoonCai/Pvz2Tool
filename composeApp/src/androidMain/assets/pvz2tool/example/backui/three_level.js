// 同类型三层嵌套弹窗（popup → popup → confirm），演示 当前.backUI 逐层把数据带回来
//
// 这里没有任何「规避复合文本约束」的 globalThis 变通：
//   - 打开下一层直接用 ui.popup / ui.confirm 的 arrow 项 onClick（普通 JS，不受 {{...}} 规则限制）
//   - 只有最内层那一条返回链接写的是 {{link|当前.backUI('...'):文字}}，
//     且其中的 JS 不含冒号（按最后一个冒号切分），完全合规，无需借助 globalThis 传参
//
// 数据回流链（每一层收到后都**再调一次** 当前.backUI，把数据继续往上送）：
//   第三层 backUI → 第三层 onResult「第二层收到」 → 再 backUI
//   → 第二层 onResult「第一层收到」   → 再 backUI
//   → 第一层（根）onResult「根收到」   → 根帧关闭整个弹窗栈
//
// 🔴 心智模型：backUI 触发的是**本帧自己**的 onResult，不是上一层的。
//    「逐层返回」靠的是「每层的 onResult 里再 backUI 一次」，而不是一次 backUI 穿透多层。
// 另：只有显式调用 当前.backUI(data) 才会触发对应层的 onResult；直接关闭只触发 onClose。

ui.popup("第一层（popup）", [
    "点箭头项打开第二层：",
    {
        type: "arrow",
        title: "打开第二层（popup）",
        onClick: function one(nav) {
            ui.popup("第二层（popup）", [
                "点箭头项打开第三层：",
                {
                    type: "arrow",
                    title: "打开第三层（confirm）",
                    onClick: function two(nav2) {
                        ui.confirm("第三层（confirm）",
                            "点下面链接返回第二层：{{link|当前.backUI('来自第三层'):返回第二层}}",
                            {
                                // 第三层 backUI 后触发：此时跑在第二层的帧上下文里
                                onResult: function (d) {
                                    ui.alert("第二层收到", "data = " + d);
                                    当前.backUI("第二层加工：" + d);
                                }
                            });
                    }
                }
            ], {
                // 第二层 backUI 后触发：此时跑在第一层的帧上下文里
                onResult: function (d) {
                    ui.alert("第一层收到", "data = " + d);
                    当前.backUI("第一层加工：" + d);
                }
            });
        }
    }
], {
    // 第一层是根弹窗：收到数据后整个弹窗栈关闭
    onResult: function (d) { ui.alert("根收到", "data = " + d); }
});

"已打开第一层弹窗";
