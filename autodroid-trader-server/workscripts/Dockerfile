# 基于Ubuntu的安卓自动化测试容器 - 使用Appium框架
FROM ubuntu:22.04

# 设置环境变量
ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
ENV APPIUM_HOME=/opt/appium

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    python3 \
    python3-pip \
    python3-dev \
    adb \
    openjdk-11-jdk \
    git \
    curl \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# 创建Android SDK目录
RUN mkdir -p $ANDROID_HOME/cmdline-tools

# 下载并安装Android SDK命令行工具
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# 接受Android SDK许可证
RUN yes | sdkmanager --licenses

# 安装必要的Android平台工具
RUN sdkmanager "platform-tools" "platforms;android-33"

# 安装Node.js (Appium需要)
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# 安装Appium
RUN npm install -g appium

# 安装Appium驱动
RUN appium driver install uiautomator2

# 安装Python依赖
RUN pip3 install --upgrade pip
RUN pip3 install \
    Appium-Python-Client==2.11.1 \
    pytest \
    airtest \
    pocoui \
    pillow \
    numpy \
    opencv-python

# 创建工作目录
WORKDIR /app

# 创建必要的目录
RUN mkdir -p /app/screenshots /app/templates /app/logs

# 复制测试脚本和配置文件
COPY test_script.py /app/
COPY config.py /app/
COPY requirements.txt /app/ 2>/dev/null || echo "Appium-Python-Client==2.11.1" > /app/requirements.txt

# 设置脚本权限
RUN chmod +x /app/test_script.py

# 创建启动脚本
RUN echo '#!/bin/bash\n\
echo "开始安卓自动化测试..."\n\
echo "手机IP: $PHONE_IP"\n\
echo "APP包名: $APP_PACKAGE"\n\
echo "Appium服务器地址: $APPIUM_SERVER"\n\
\n\
# 启动Appium服务器（后台运行）\n\
appium --port 4723 --base-path /wd/hub &\n\
echo "等待Appium服务器启动..."\n\
sleep 10\n\
\n\
# 等待手机连接\n\
adb connect ${PHONE_IP:-192.168.1.100}:5555\n\
\n\
# 运行测试\n\
cd /app\n\
python3 -m pytest test_script.py -v\n\
\n\
echo "测试完成，查看截图: /app/screenshots"\n\
' > /app/start_test.sh && chmod +x /app/start_test.sh

# 暴露端口
EXPOSE 5555 4723

# 设置默认命令
CMD ["/app/start_test.sh"]