# Student Beans 官方流程研究

核查日期：2026-10-03。范围：官方 Google Play、Student Beans 官网和帮助中心。本轮没有安装或启动 App，没有登录、注册、上传材料、授权定位/相机/通知，也没有领取或实际核销。以下“官方说明”与“已查看公开截图”不等于当前 Android 版本亲测。

## 入口与地区限制

- [官方 Google Play](https://play.google.com/store/apps/details?id=com.studentbeans.studentbeans&hl=en)：开发者 The Beans Group Ltd，包名 `com.studentbeans.studentbeans`。已下载并查看图标，紫底白色 SB；不能由商店图标推断启动页。仅从商店解析公开图片，没有下载第三方 APK。
- [官方 App 可用市场说明](https://help.studentbeans.com/hc/en-us/articles/115000608225-Is-there-a-Student-Beans-app)列英、美、法、德、加、澳、意、西、爱尔兰、荷兰 10 个市场，没有中国；[网站国家说明](https://help.studentbeans.com/hc/en-us/articles/115000593409-Is-Student-Beans-available-in-my-country)包含中国。中文网站存在不等于大陆商店可直接安装、所有优惠适用中国。
- 后续只通过官方商店尝试安装。若要求账号或地区不支持，记录阻塞；不创建账号、不切换地区绕过、不找第三方 APK。

## 流程与证据边界

| 阶段 | 官方资料能确认的行为 | 本轮未能亲测 |
|---|---|---|
| 安装 → 首次启动 | 官方提供 Android/iOS 商店入口；图标见本地素材 | 安装包版本、启动画面、首次说明顺序、隐私弹窗、可否跳过登录、权限弹窗时机 |
| 账号注册/登录 | 官方建议用个人邮箱注册；已注册账号以邮箱和密码登录。[登录排障](https://help.studentbeans.com/hc/en-us/articles/25286843994908-Troubleshooting-Login-and-Verification-Wizard-Issues)、[续期说明](https://help.studentbeans.com/hc/en-us/articles/25286885025692-Verifying-or-Renewing-Your-Student-Beans-Membership) | 当前输入字段、第三方登录、表单校验、登录后的精确落点；公开登录地址本轮工具不可访问 |
| 选择/关联学校 | 在学生状态验证页搜索学校完整名称；找不到时提供添加学校入口，之后仍须验证。[学校缺失帮助](https://help.studentbeans.com/hc/en-us/articles/115000593369-My-institution-is-not-on-the-list-can-I-still-verify-my-student-status) | 当前 Android 学校列表、国内学校覆盖、学校绑定后是否能更换 |
| 学生资格验证 | 官方列学校学习门户、学校邮箱、人工上传证明三种途径；不是把普通邮箱验证直接当学生认证。[人工验证说明](https://help.studentbeans.com/hc/en-us/articles/360022218420-How-do-I-manually-verify-my-student-status) | 门户跳转、真实邮件送达、审核结果、地区可用途径 |
| 邮箱验证失败 | 提示检查垃圾邮件、重新输入地址；仍失败可转人工方法。[未收到邮件](https://help.studentbeans.com/hc/en-us/articles/115000608045-I-haven-t-received-a-verification-email) | 重发冷却、防重复点击、链接失效后的实际界面 |
| 人工材料 | 学校签发的学生证、录取证明或成绩单，须能证明姓名、学校、当学年有效性；官方要求遮盖无关个人信息。[没有学校邮箱](https://help.studentbeans.com/hc/en-us/articles/360007269679-Don-t-have-an-institution-email-address) | 文件选择权限、当前格式/大小限制、提交后待审/驳回/通过界面 |
| 验证成功 → 返回优惠 | 官方称验证成功后自动激活数字学生卡，随后可在网站或 App 使用优惠。[激活说明](https://help.studentbeans.com/hc/en-us/articles/25286885027868-How-to-Activate-Your-Student-Beans-Account) | 是否自动回到此前优惠详情、是否保留选中品牌、跨浏览器或邮件深链恢复 |
| 线上领取 | 搜索品牌，Get Discount 后取得优惠码或进入已折价的教育商店；优惠码用于品牌网站结账。[线上/线下区别](https://help.studentbeans.com/hc/en-us/articles/25292291794716-How-to-use-Student-Beans-Online-vs-In-store) | 复制提示、外跳与返回、真实优惠码生成、使用成功 |
| iD Wallet 与品牌卡 | iD Wallet → Add iD → 选择品牌 iD → 打开卡片查看权益 → Redeem 展示品牌学生 iD。[店内领取步骤](https://help.studentbeans.com/hc/en-us/articles/360004824400-How-do-I-redeem-in-store) | 当前卡片添加/删除动效、离线状态、真实商家接受情况 |
| 店内展示与扫描 | 官方更完整说明提及照片和验证计时器；部分品牌 iD 有二维码、条形码或唯一代码，按品牌而异，不能说所有店都扫同一种码。[线上/线下区别](https://help.studentbeans.com/hc/en-us/articles/25292291794716-How-to-use-Student-Beans-Online-vs-In-store) | 倒计时长度、码刷新机制、服务器验签、POS 核销回执、历史记录 |
| 附近 | 官方帮助说底部 In-Store 可浏览附近权益，开启定位可使结果更准确。[附近说明](https://help.studentbeans.com/hc/en-us/articles/360004824560-How-does-the-near-me-feature-work) | 拒绝定位后的实际行为、手动城市入口、门店详情、导航路径 |

“绑定”在已查材料中是关联学校/验证途径，以及向钱包添加品牌 iD；没有证据表明必须绑定支付卡。不能将两者混同。

## 已实际查看的公开像素

目录：`D:\Codex\2026-10-03\task\studentbeans-reference`。

- `icon.png`：紫色背景、白色 SB，来自 Play 图标。
- `play-01.png`—`play-08.png`：Play 宣传图片，已逐张查看。`play-03` 展示品牌优惠与店内按钮；`play-04` 展示首页和通知；`play-05` 展示真实地图式界面、品牌地点标记和分类；`play-06` 展示照片学生 ID；`play-07` 展示首页内容。其余主要是营销画面。图片分辨率有限，且手机框具有 iOS 外观，不能称为当前 Android 实机截图。
- `verification-methods.png`：三种验证方法的列表，每项有图标、解释和箭头。
- `manual-guidelines.png`：先解释证明材料与四项要求，再提供继续按钮；不能接受的第三方学生卡类型单独列出。
- `manual-upload.png`：学校选择、上传区域、提交按钮、切换验证方式。旧图显示 JPG/PNG/PDF、40 MB；仅记录历史截图，不把该限制当作当前接口规格。
- `email-pending.png`：未收到邮件的展开说明，可重新输入邮箱。
- `institution-search.png`、`institution-methods.png`：学校搜索与后续方法的官方旧截图。

帮助截图源文件名带 `2021-06-02`，属于旧版流程证据。上述图片仅为研究参考，不应放进 VaStudents 产品或复制 Student Beans 品牌视觉。

## 需要保留的差异与未知

1. 人工审核时间存在官方口径差异：人工验证帮助写最多 3 天，没有学校邮箱的帮助写少于 24 小时。不要承诺固定审核时长。
2. 两份官方店内帮助分别称底部入口为 iD Wallet 和右下 iD，可能代表不同版本；必须由后续运行截图确认。
3. [官方店内被拒帮助](https://help.studentbeans.com/hc/en-us/articles/360004861739-I-was-refused-in-store-discount)要求先查适用日期、商品等条件。数字学生 ID 不等于所有店、所有商品均接受优惠。
4. 本轮没有发现能证明完整当前 Android 首次启动到真实核销的官方连续录像；不得用帮助文字或宣传图冒充实测。

## 对 VaStudents 的结构建议

- 账号、学生资格、品牌权益、核销凭证和核销结果分别建模；公开优惠浏览不依赖虚构的已认证身份。
- 资格验证至少预留未开始、处理中、待补资料、已通过、已过期、失败等状态；当前没有验证服务时明确不可用，不收集材料。
- 未来认证成功应返回用户发起验证的优惠/钱包卡，并保留上下文；这是我们的设计建议，不是本轮观察到的 Student Beans 行为。
- 钱包中的品牌卡必须有具体商户授权及权益规则；演示码持续显式标测试，不产生真实核销记录。
- 学習其搜索、收藏、线上外跳与品牌钱包的功能关系，VaStudents 继续采用已确认的独立浅色/近黑视觉，不复制 SB 图标、文案或营销素材。

## 实际官方安装入口尝试

独立 Android16 AOSP 模拟器 emulator-5582 没有 com.android.vending。2026-10-03 通过官方 market://details?id=com.studentbeans.studentbeans 发起安装入口，系统返回 unable to resolve Intent。没有已登录的 Play 商店设备，未创建账号/切地区/安装第三方 APK，仍未亲测 Student Beans 首次启动。证据：05_Output/studentbeans-install-attempt.txt。
