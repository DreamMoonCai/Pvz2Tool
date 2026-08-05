var records = [];
var keys = storage.keys();

for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    if (key.startsWith("used_giftcode_")) {
        var record = storage.get(key);
        if (record && record.code) {
            records.push(record);
        }
    }
}

if (records.length === 0) {
    ui.alert("兑换记录", "暂无兑换记录，快去兑换吧！");
    "暂无记录";
} else {
    var message = "共 " + records.length + " 条兑换记录：\n\n";
    for (var i = 0; i < records.length; i++) {
        var r = records[i];
        message += "📦 " + r.code + "\n   奖励：" + r.reward + "\n   时间：" + r.time + "\n\n";
    }

    ui.alert("兑换记录", message);
    "查看完成: " + records.length + " 条记录";
}