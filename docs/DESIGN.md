# 设计与验收边界

颜色 tokens：Ink #191B1D、Muted #63686E、Surface #F5F6F7、Line #E7E9EB、白底。系统 sans 常规字重用于正文；sans-serif-medium 用于主标题和必要操作，禁止全屏粗体。首页主标题24sp、页面标题28sp，优惠标题 19sp，段落标题16sp，正文14–15sp，辅助信息12–13sp。基准边距22dp，间距4/8/12/16/24/28dp；按钮至少50dp，圆角16dp，分段圆角24dp、选中块白色。

MainActivity 的 text/button/notice/paragraph/header/col/row 是统一基础组件。黑底白字主按钮、浅底次按钮；图标使用统一1.7单位圆角线条。优惠列表的浅底容器不再嵌套卡片，无投影或渐变。系统字号缩放不禁用，按钮随文本增高，搜索框采用最小高度和自适应高度。品牌启动页使用系统 SplashScreen 图标，首次说明可直接进入访客首页。

用户四张 Library 图的读取仍阻塞：官方 materialize helper 已接收授权传输，Windows Python 因 os.setxattr 缺失失败，最终路径不存在；没有绕过 helper 或伪称像素已查看。此次实现依据用户文字规范；另有已查看的 Student Beans 官方帮助与宣传像素，仅作流程研究，不复制品牌。

视觉必须由实际 APK 截图审阅；开发者实现和自动化测试不是用户审美验收。用户尚未验收。动效以运行证据为准，静态图不能证明流畅度。
