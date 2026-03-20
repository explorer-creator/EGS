@echo off
setlocal enabledelayedexpansion

:: 核心配置
set HW_GROUP=com.huawei.isc.xdm-f
set HW_VER=2.25.040.10-sdk-jdk17

for %%i in (*.jar) do (
    set "fname=%%~ni"
    
    :: 检查是否是华为 xdm-f 开头的核心包
    echo %%i | findstr /C:"xdm-f-" >nul
    if !errorlevel! equ 0 (
        echo [Installing Huawei Core]: %%i
        :: 自动去掉版本号后缀获取 ArtifactId
        set "aid=!fname:-2.25.040.10-sdk-jdk17=!"
        call mvn install:install-file -Dfile="%%i" -DgroupId=%HW_GROUP% -DartifactId=!aid! -Dversion=%HW_VER% -Dpackaging=jar
    ) else (
        echo [Installing Third-party]: %%i
        :: 不带坐标参数，让 Maven 自动读取 JAR 内部的 pom.xml 坐标
        call mvn install:install-file -Dfile="%%i"
    )
)
pause