# 本地签名与备份

当前仅有本项目新生成的 Android Debug 签名，未创建正式 JKS，也未复用其他项目凭据。

- 本地文件：`D:\Codex\01_App-应用开发\007_VaStudents\01_Repositories\VaStudents\.local\android\debug.keystore`
- 类型：工具链开发证书，标准 debug keystore；当前文件不是 JKS 魔数开头（非 FEEDFEED），不把它称为正式 JKS。
- 非秘密 alias：androiddebugkey
- 证书 SHA-1：D2D71BE1950BE82C6C10CE3A23EFB63C5F5510B4
- 证书 SHA-256：17272ADE16FDE418993CFAC9BFC91058B455CB70EDA0FF1347A1D4200F94B8C2
- debug APK 已用 apksigner 验证，v2签名有效。最终产物另附 SHA256SUMS，APK哈希与证书指纹是两种不同值。

`.local/`、`*.jks`、`*.keystore`、`*.dpapi`、signing.properties 均排除版本控制。不会上传任何签名/口令/密钥；本文不包含口令。Debug证书不能承担正式分发签名身份。

## 正式签名待确认的具体事项

需用户确认建立 VaStudents 全新正式签名身份，建议路径为项目 `00_Project-项目说明\Local-Private\signing\VaStudents-release.jks`，alias `vastudents-release`。确认后由本机遮蔽输入或安全生成流程配置口令，以当前Windows用户DPAPI保护，并验证访问控制；不得沿用 MyChangLi 或其他项目证书。本次没有创建或替换正式身份。

## 备份与恢复

用户选择受保护的离线介质或加密密码管理器备份区后，再备份正式 JKS。JKS 与可跨设备恢复的口令应分开保管，保留至少两份受控离线副本；DPAPI 文件不能单独当作跨电脑恢复方案，它依赖原Windows用户。记录alias、证书SHA256、创建时间与版本，先验证副本能列出相同证书，再做一次签名恢复演练。不要把JKS/口令送入聊天、Git、APK或普通项目压缩包。

当前 debug.keystore 若希望后续直接覆盖安装开发APK，也应保持稳定；丢失后可重建开发签名，但现有安装可能需要卸载。未执行任何未经指定目的地的备份复制。
