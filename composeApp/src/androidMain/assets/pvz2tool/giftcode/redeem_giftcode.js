var code = this.all.welfare.giftcode_input.value.trim();
var saves = pvz.saves.load();
var datas = saves.objects[0].objdata;
var welfare_status = this.all.welfare.welfare_status;

var saves_info = this.all.saves_info;
var GoldQuantity = saves_info.GoldQuantity;
var DiamondQuantity = saves_info.DiamondQuantity;

// 植物碎片兑换辅助函数
// plant: 植物对象 通过 pvz.植物.植物名称 获取
// shardCount: 碎片数量
function addPlantShard(plant, shardCount) {
    console.log("为植物 " + plant.name + " (序号: " + plant.order + ") 添加碎片×" + shardCount);

    // datas.ppr 数组存储植物碎片: pi = 植物序号, pc = 碎片数量
    if (!datas.ppr) {
        datas.ppr = [];//存档还没有碎片数组的话则创建数组
    }

    // 查找是否已拥有该植物碎片
    for (var i = 0; i < datas.ppr.length; i++) {
        if (datas.ppr[i].pi == plant.order) {
            datas.ppr[i].pc += shardCount;
            console.log("更新 " + plant.name + " 碎片: " + datas.ppr[i].pc);
            saves.save();
            return;
        }
    }

    // 不存在则添加新记录
    datas.ppr.push({
        pi: plant.order,
        pc: shardCount
    });
    console.log("新增 " + plant.name + " 碎片×" + shardCount);
    saves.save();
}

function pvz2026() {
    datas.gg += 300;
    datas.g = (datas.g.解密 + 300).加密;
    datas.c = (datas.c.解密 + 5000).加密;
    saves.save();
    GoldQuantity.call();
    DiamondQuantity.call(); // 更新存档信息栏目的信息
}

function 获取豌豆射手碎片() {
    // 兑换豌豆射手碎片×10
    addPlantShard(pvz.植物.豌豆射手, 10);
}

function 获取向日葵碎片() {
    // 兑换向日葵碎片×10
    addPlantShard(pvz.植物.向日葵, 10);
}

function 获取樱桃炸弹碎片() {
    // 兑换樱桃炸弹碎片×5
    addPlantShard(pvz.植物.樱桃炸弹, 5);
}

function 其他() {}

var validCodes = {
    "PVZ2026": {
        reward: "钻石×300 + 金币×5000",
        name: "新手豪华礼包",
        call: pvz2026
    },
    "GEMS": {
        reward: "钻石×666",
        name: "钻石礼包",
        call: 其他
    },
    "PEASHOOTER": {
        reward: "豌豆射手碎片×10",
        name: "豌豆射手碎片包",
        call: 获取豌豆射手碎片
    },
    "SUNFLOWER": {
        reward: "向日葵碎片×10",
        name: "向日葵碎片包",
        call: 获取向日葵碎片
    },
    "CHERRY": {
        reward: "樱桃炸弹碎片×5",
        name: "樱桃炸弹碎片包",
        call: 获取樱桃炸弹碎片
    }
};

function redeem() {
    if (!code) {
        ui.alert("兑换失败", "请先输入礼品码！");
        return "未输入礼品码";
    }

    // 模拟礼品码校验（实际使用时替换为真实兑换逻辑）
    var upperCode = code.toUpperCase();
    if (validCodes[upperCode]) {
        var record = storage.get("used_giftcode_" + upperCode);
        if (record && record.code) {
            ui.alert("兑换失败", "您已使用过此礼品码！");
            return "重复使用礼品码";
        }
        var gift = validCodes[upperCode];
        gift.call();
        console.log("兑换成功: " + gift.name);
        console.log("获得奖励: " + gift.reward);

        // 保存已使用的礼品码到 storage
        storage.set("used_giftcode_" + upperCode, {
            code: upperCode,
            reward: gift.reward,
            time: new Date().toLocaleString()
        });
        welfare_status.call();
        ui.alert("兑换成功！🎉", "恭喜获得：「" + gift.name + "」\n" + gift.reward);
        return "兑换成功: " + gift.name;
    } else {
        console.log("无效礼品码: " + code);
        ui.alert("兑换失败", "礼品码不存在或已过期，请检查后重试！");
        return "无效礼品码";
    }
}
redeem()
