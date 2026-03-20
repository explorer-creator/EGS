@echo off
set GROUP_ID=com.huawei.isc.xdm-f
set VERSION=1.0.0-SNAPSHOT

for %%i in (*.jar) do (
    setlocal enabledelayedexpansion
    set FILENAME=%%~ni
    :: 自动截取文件名作为 ArtifactId（假设文件名以版本号结尾）
    set ARTIFACT_ID=!FILENAME:-1.0.0-SNAPSHOT=!
    
    echo Installing !ARTIFACT_ID! ...
    call mvn install:install-file -Dfile="%%i" -DgroupId=%GROUP_ID% -DartifactId=!ARTIFACT_ID! -Dversion=%VERSION% -Dpackaging=jar
    endlocal
)
pause