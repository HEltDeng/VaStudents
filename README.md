# VaStudents

中国学生公开优惠聚合的原生 Android 开发预览。名称精确为 **VaStudents**。本项目独立于其他校园与 YEATS 项目，无 GitHub remote、云部署或正式发布。

## 本地路径

- 项目：`D:\Codex\01_App-应用开发\007_VaStudents`
- 仓库：`D:\Codex\01_App-应用开发\007_VaStudents\01_Repositories\VaStudents`
- 可交付产物/运行证据：项目 `05_Output`
- Android Java 原生 Views，AGP 9.2.0、Gradle 9.4.1、JDK 17；compileSdk 36.1、targetSdk 36、minSdk 26。

## 构建

配置 JDK17/JAVA_HOME 和 Android SDK，在忽略的 `local.properties` 中配置 `sdk.dir`；安装 `platforms;android-36.1` 和 `build-tools;36.1.0`。

```powershell
Set-Location 'D:\Codex\01_App-应用开发\007_VaStudents\01_Repositories\VaStudents'
.\scripts\build.ps1
# 如本机 Java 无法直连，显式使用已有代理（仅影响本进程）
.\scripts\build.ps1 -ProxyHost 127.0.0.1 -ProxyPort 7897
```

脚本始终设置项目专属 ANDROID_USER_HOME，避免复用其他项目签名。APK 在 `app/build/outputs/apk/{preview,dev}/debug/`。自动化设备测试：向脚本传 `-Tasks connectedPreviewDebugAndroidTest,connectedDevDebugAndroidTest`，只使用明确指定的测试模拟器，勿自动操作个人手机。

## 两种构建

| 构建 | 包名 | 开发身份/演示审批 | 用途 |
|---|---|---|---|
| previewDebug | cn.vastudents.app | 编译常量 false + 业务入口抛拒绝 | 无开发绕过的体验预览，仍是 debug 签名 |
| devDebug | cn.vastudents.app.dev | 仅合成测试，始终有显式标识 | 验证首次说明→访客→测试账户→材料→审核→返回钱包/二维码 |

两个包名独立存储。真实登录、真实材料上传、权威资格验证、合作商户权益与真实核销均未上线。体验版不应被称为正式发行版。`verification_backend` 是未部署、未接 App 的受控服务端领域/存储原型；它验证生产模式拒绝所有开发路径，不能假装已提供生产认证。

## 已有功能

首次品牌/隐私说明、访客浏览、搜索分类、四项可追溯官方公开优惠、详情与来源外跳、收藏、个人偏好、手动选城、真实 OSM 地图嵌入与失败提示、具体地点严格适用数据模型、ID Wallet、合成身份生命周期与测试二维码、核销演示与真实记录分离。已收录长沙岳麓书院一个真实学生票地点，地图标记为景点中心而非入口；官方到达路线外跳。不显示虚构商户或营业信息。

见 `docs/DESIGN.md`、`docs/DATA_AND_MAP.md`、`docs/SIGNING.md`、`docs/VERIFICATION_BOUNDARIES.md` 和 `docs/STUDENT_BEANS_RESEARCH.md`。最终实测范围以 `05_Output/DELIVERY.md` 为准。
