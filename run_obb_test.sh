#!/bin/bash
# OBB 打包测试脚本

PROJECT_DIR="/Users/macbookpro/AndroidStudioProjects/Pvz2Restart"
cd "$PROJECT_DIR" || exit 1

echo "====================================="
echo "OBB 打包测试"
echo "====================================="

# 设置 classpath
CP="composeApp/build/classes/kotlin/jvm/main"
for jar in $(find composeApp/build -name "*.jar" 2>/dev/null); do
    CP="$CP:$jar"
done

# 运行测试
echo -e "\n运行 MainObbTest..."
java -cp "$CP" io.github.dreammooncai.pvz2tool.pop.MainObbTest 2>&1
