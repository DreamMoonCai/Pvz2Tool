// demo.js - 用于测试 js.run("demo.js")
// 此文件会被 js.run("demo.js") 执行

var message = "你好 from demo.js!";
var timestamp = new Date().toLocaleString();

// 返回一个包含信息的对象
var result = {
    message: message,
    timestamp: timestamp,
    version: "V1.0.0",
    author: "Dream moon Cai"
};

// 返回值会作为 js.run() 的结果
result;
