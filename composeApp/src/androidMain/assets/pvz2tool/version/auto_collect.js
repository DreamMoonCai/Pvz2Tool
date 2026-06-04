let config = data.dynamic.packages.activityconfig.load()
config.objects[0].objdata.AutoSunCollect = this.checked
if (this.checked) {
    console.log("阳光自动收集", "已开启")
} else {
    console.log("阳光自动收集", "已关闭")
}

config.save()