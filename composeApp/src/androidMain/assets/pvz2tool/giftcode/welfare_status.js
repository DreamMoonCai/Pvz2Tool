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
"共兑换过 " + records.length + " 个礼品码"