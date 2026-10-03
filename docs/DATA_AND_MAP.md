# 真实优惠、地图与维护

2026-10-03 核对以下公开官方页面，无登录、资格验证、领取、交易或合作授权：

- Apple 教育优惠：https://www.apple.com.cn/cn-edu/store
- Office 365 A1 教育版：https://www.microsoft.com/zh-cn/education/products/office
- Apple Music 学生订阅：https://support.apple.com/zh-cn/106008
- 岳麓书院学生半价票：https://ylsy.hnu.edu.cn/wbly/cgdn/zxdp.htm

完整条件在 assets/offers.json；线上条目未编造具体价格；岳麓书院20元学生票有官方现行购票须知支持。未编造统一截止日、赠品、学校覆盖、门店或营业时间。verifiedAt 仅是公开页面核对日期；30天后显示待重新核实，明确到期日期到期后显示已过期，下架优先。sourceStatus 必须 public_page_checked 才能显示已核实。ID/来源去重，测试内容不进入公开优惠列表。人工维护种子目录，未承诺后台实时采集；采集系统后续需遵守来源许可与 robots，不绕过登录/反爬。

Store 模型要求具体名称/地址/城市/来源/核实时间/WGS84坐标。只有 offer.channel=offline 且 storeIds 显式包含该门店才适用；不能由品牌推断所有分店参与。当前仅岳麓书院一个真实地点，按城市与有效优惠筛选，其他城市清楚显示空态。优惠适用于该书院，不扩展到湖南大学其他场所；没有商户合作。

## 地图方案

当前采用 OpenStreetMap 官方 `export/embed.html`，在 Android 系统 WebView 中交互显示。只在用户打开附近时加载城市区域，不调用定位 API；Manifest 仅 INTERNET 权限。浏览器默认缓存，HTTPS，不忽略证书错误，不注入 JS bridge，不允许文件或 content 访问。显示原始地图署名和外部版权链接；不抓取、不预下载、不提供离线地图包。

无已核实条目时使用粗略 WGS84 城市中心。已选具体地点显示一个可追溯标记，不是用户位置。地图显示的普通 POI 不代表学生优惠参与门店。地图加载状态检查可见瓦片，不将缓存命中称为实时联网。岳麓书院地点标记来自一次 Nominatim 公开查询（OSM way 272516791，WGS84 28.1839094,112.9351255），是景点中心而不是已核实入口坐标。到达路线按钮打开官方交通说明，另有外部地图查看地点；没有自动定位、入口精确导航或内置逐向导航。完整来源原始记录见 YUELU_SOURCE_EVIDENCE.json。

OSM 服务政策：https://operations.osmfoundation.org/policies/tiles/ 。公共瓦片无 SLA，可限制访问，不宜视作中国正式商业服务保障；正式国内分发前需确认所需地图合规和可用性，不做法律结论。

## 可切换高德，尚未申请

https://lbs.amap.com/api/android-sdk/guide/create-project/get-key ：独立应用，选择 Android平台SDK，填写包名与实际 SHA1。体验包 cn.vastudents.app；开发包 cn.vastudents.app.dev 需分别绑定，不复用其他项目 Key。当前调试 SHA1 见 SIGNING.md；正式 SHA1 尚无，必须用未来正式证书。

https://lbs.amap.com/upgrade ：本次官方页面列技术服务许可基础版50,000元/年、高级版100,000元/年；不要推断每种用途都需购买此项或免费Key已授权商业使用。先明确运营主体、商业用途及地图显示/搜索/导航范围，通过官方工单核对实际适用许可、配额和报价，用户批准收费/条款后申请。没有创建账户或购买。高德坐标GCJ-02不能直接复用WGS84门店坐标，接入时按官方SDK转换并实测目的地。

## 地图分项验收

底图瓦片曾实测HTTP200；具体学生优惠按城市筛选、地点详情、关联优惠及官方路线Intent另行设备测试。地图标记只代表该项官方公开优惠适用地点，不代表商家接受VaStudents核销码。官方未授权第三方代售/代购/代销门票，本应用不售票或代预约。未验证预约余票、现场入院、实际入口坐标及中国大陆无代理网络稳定性。
