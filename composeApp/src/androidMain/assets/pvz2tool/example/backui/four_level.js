// 四层嵌套弹窗（popup → confirm → popup → confirm），演示 当前.backUI 逐层把数据带回来
//
// 说明：
//   - 打开下一层一律用普通 JS（arrow 的 onClick / confirm 的 onConfirm），不受 {{...}} 约束
//   - 只有最内层那一条返回链接写的是 {{link|当前.backUI('...'):文字}}，
//     且其中的 JS 不含冒号（按最后一个冒号切分），合规
//   - 四层混用了 popup 与 confirm 两种弹窗，验证同类型 / 异类型嵌套都能逐层返回
//
// 数据回流链（每层收到后再 backUI 一次，逐层往上送）：
//   第四层 backUI → 第四层 onResult「第三层收到」 → 再 backUI
//   → 第三层 onResult「第二层收到」 → 再 backUI
//   → 第二层 onResult「第一层收到」 → 再 backUI（此时第二层早已离场，只补发 onResult）
//   → 第一层（根）onResult「根收到」 → 根帧关闭整个弹窗栈
//
// 🔴 心智模型：backUI 触发的是**本帧自己**的 onResult，不是上一层的。
//    注意第二层是 confirm：它的 onConfirm 里**不调** backUI，而是打开第三层；
//    等第四层的数据一路回流到第二层的 onResult 时，第二层早已关闭，
//    此时再 backUI 仍有效（帧已离场则只补发 onResult，不会重复弹栈）。

ui.popup("第一层（popup）", [
    "点箭头项打开第二层：",
    {
        type: "arrow",
        title: "打开第二层（confirm）",
        onClick: function (nav) {
            ui.confirm("第二层（confirm）", "点确认打开第三层 popup", {
                // 这里**不要** backUI，否则第二层会直接带着数据离场、看不到第三层
                onConfirm: function () {
                    ui.popup("第三层（popup）", [
                        "点箭头项打开第四层：",
                        {
                            type: "arrow",
                            title: "打开第四层（confirm）",
                            onClick: function (nav2) {
                                ui.confirm("第四层（confirm）",
                                    "点下面链接返回第三层：{{link|当前.backUI('来自第四层'):返回第三层}}",
                                    {
                                        onResult: function (d) {
                                            ui.alert("第三层收到", "data = " + d);
                                            当前.backUI("第三层加工：" + d);
                                        }
                                    });
                            }
                        }
                    ], {
                        onResult: function (d) {
                            ui.alert("第二层收到", "data = " + d);
                            当前.backUI("第二层加工：" + d);
                        }
                    });
                },
                // 第二层此刻早已关闭（点确认后就弹栈了），但它注册的 onResult 仍然有效：
                // 数据一路回流到这里时，再 backUI 一次会直接补发 onResult（帧已离场不重复弹栈）
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
