// 存档标记 curuser 读写示例（外置 files 目录下的 sexy.json）
//
// 背景：有说法是游戏把「当前选中的存档」记在 sexy 文件根层级的 curuser 字段。
// 工具不写死该路径与格式，完全由 JS 自行决定。下面两个示例可分别取用。
//
// 路径占位符：$ANDROID_FILES = /storage/emulated/0/Android/data/<包名>/files
// 文件名按实际游戏调整：若游戏文件就叫 "sexy"（无扩展名），把下面的 sexy.json 改成 sexy。

// ============================================================
// 示例一：读取 sexy.json 根层级 curuser 字段（示例值为 2）
// ============================================================
// 纯 JSON 文件用 file.readText + 引擎内置 JSON.parse，不依赖扩展名：
var path = "$ANDROID_FILES/sexy.json";
var text = file.readText(path);
var sexy = JSON.parse(text);
var curuser = sexy.curuser;                       // 根层级字段，示例值 2
console.log("curuser = " + curuser);              // 输出：curuser = 2

// 若文件名确实以 .json 结尾，也可用 rton.load（会自动注入 save()，见示例二）：
// var sexy = rton.load("$ANDROID_FILES/sexy.json");
// console.log("curuser = " + sexy.curuser);

// 如需「界面显示时按游戏的 curuser 定位工具槽位」，可接着调用（注意 0 基 / 1 基）：
// if (typeof curuser === "number") pvz.saves.setSlot(curuser);   // 若游戏是 1 基则传 curuser - 1


// ============================================================
// 示例二：切换子存档后，把工具选中的槽位写回 sexy.json 的 curuser
// 把本示例配置到 ui.save.onSaveChangedJs / onSaveChangedJsPath 即可自动执行
// ============================================================
var slot = pvz.saves.slot();                      // 当前选中槽位（0 基）
var path2 = "$ANDROID_FILES/sexy.json";
var obj = JSON.parse(file.readText(path2));
obj.curuser = slot;                               // 根层级字段改为选中的槽位
file.writeText(path2, JSON.stringify(obj));       // 顶层两参版本；不要写成 file.resolve(path2).writeText(obj)
console.log("已把 curuser 写为 " + slot);

// 等价写法（文件名必须是 .json，rton.load 靠扩展名判断是否当 JSON 读）：
// var obj2 = rton.load(path2);
// obj2.curuser = slot;
// obj2.save();                                   // 写回原路径；也可 obj2.save("$ANDROID_FILES/sexy.json")

// 注意：示例二中若调用 pvz.saves.setSlot()，其再次触发的存档变更事件会被自动跳过，不会无限递归。
