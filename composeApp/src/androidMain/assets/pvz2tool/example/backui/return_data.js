// 嵌套弹窗 backUI 测试：popup（第一层，根帧） → confirm（第二层）
//
// 第二层有三种「带回数据」的方式，都会触发**第二层自己**注册的 onResult（即下面写的「第一层收到」）：
//   1) 复合文本链接（就地求值）：{{link|当前.backUI('来自第二层的数据'):...}}
//   2) 确认按钮：onConfirm 里写 当前.backUI("确认带回")
//   3) 取消按钮 / 点外部：onCancel 里写 当前.backUI("取消带回")
//
// 🔴 关键心智模型：**backUI 是「本帧带 data 离场，并把 data 交给打开本帧时注册的那个 onResult」**
//    —— 所以「第二层调 backUI」触发的是第二层的 onResult，不是第一层的。
//    想让数据继续往上走，就在收到数据的 onResult 里再调一次 当前.backUI（见下面第 30 行）。
//
// 反之：若某个回调里**没有**调用 当前.backUI，则只触发本弹窗的 onClose，
// 上一层的 onResult **不会**被触发 —— onResult 的语义是「带回数据」，不是「关闭通知」。
//
// 要点：
//   1) 当前.backUI 在「复合文本链接」与「弹窗回调」两种位置都可用，语义完全一致
//   2) 富文本 {{...}} 不支持嵌套，所以用 ui.popup 的 arrow 项 onClick 打开第二层
//   3) {{link|JS代码:显示文字}} 中的 JS 代码不能出现冒号（按最后一个冒号切分），
//      所以这里的 JS 只调用 当前.backUI(...)，不带对象字面量

var lvl2 = "{{link|当前.backUI('来自第二层的数据'):点我返回上一层并带回数据}}";

ui.popup("第一层（popup）", [
    "点下面的箭头项打开第二层弹窗：",
    {
        type: "arrow",
        title: "打开第二层弹窗（confirm）",
        onClick: function (nav) {
            ui.confirm("第二层（confirm）", lvl2, {
                // 第二层 backUI 后触发这里。收到数据后**继续向上回传一层**：
                // 此刻回调正跑在第一层的帧上下文里，所以 当前.backUI 退掉的是「第一层」自己，
                // 数据会交给下面 ui.popup(...) 注册的 onResult。
                onResult: function (d) {
                    ui.alert("第一层收到", "data = " + d);
                    当前.backUI("第一层加工：" + d);
                },
                // 在普通回调里直接调用 backUI（不是 {{link}}，无需顾虑冒号约束）
                onConfirm: function () {
                    控制台.log("执行确定")
                    当前.backUI("确认带回");
                 },
                onCancel: function () { 当前.backUI("取消带回"); }
            });
        }
    },
    "第二层显式 backUI 后，第二层注册的 onResult 才会触发"
], {
    // 第一层是根弹窗（没有上一层）。根帧的 backUI = 把 data 交回本回调，然后关闭整个弹窗栈。
    onResult: function (d) { ui.alert("脚本收到（根帧已关闭）", "data = " + d); }
});

"已打开第一层弹窗";
