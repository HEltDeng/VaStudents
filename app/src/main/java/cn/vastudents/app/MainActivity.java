package cn.vastudents.app;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

public class MainActivity extends Activity {
  static final int INK = Color.rgb(25, 27, 29),
      MUTED = Color.rgb(99, 104, 110),
      SURFACE = Color.rgb(245, 246, 247),
      LINE = Color.rgb(231, 233, 235);
  final OfferRepository repository = new OfferRepository();
  final WalletState wallet = new WalletState();
  DevelopmentVerification verification;
  String flow = "", returnIntent = "";
  long qrExpiresAt = 0;
  String qrPayload = "";
  DevelopmentVerification.State renderedVerification = DevelopmentVerification.State.NONE;
  final Handler handler = new Handler(Looper.getMainLooper());
  LinearLayout root, content, nav, list;
  ScrollView scroll;
  WebView map;
  SharedPreferences prefs;
  Set<String> favorites = new HashSet<>();
  String tab = "发现", category = "全部", query = "", detailId = null, city = "长沙";
  String storeId = null;
  boolean ready = false, loadFailed = false, destroyed = false;
  int loadGeneration = 0;
  long lastExternal = 0;
  int scrollPosition = 0;
  final String[] cities = {"长沙", "北京", "上海", "广州", "深圳", "成都", "杭州", "武汉"};
  // WGS84 city centres, not device locations or merchant pins.
  final double[][] centres = {
    {112.9388, 28.2282},
    {116.4074, 39.9042},
    {121.4737, 31.2304},
    {113.2644, 23.1291},
    {114.0579, 22.5431},
    {104.0665, 30.5723},
    {120.1551, 30.2741},
    {114.3054, 30.5931}
  };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    prefs = getSharedPreferences("vastudents", MODE_PRIVATE);
    favorites = new HashSet<>(prefs.getStringSet("favorites", Collections.emptySet()));
    city = prefs.getString("city", "长沙");
    verification = new DevelopmentVerification(this);
    if (b != null) {
      flow = b.getString("flow", "");
      returnIntent = b.getString("returnIntent", "");
      tab = b.getString("tab", "发现");
      category = b.getString("category", "全部");
      query = b.getString("query", "");
      detailId = b.getString("detailId");
      storeId = b.getString("storeId");
      scrollPosition = b.getInt("scrollPosition", 0);
    }
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
    render();
    load();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("flow", flow);
    b.putString("returnIntent", returnIntent);
    b.putString("tab", tab);
    b.putString("category", category);
    b.putString("query", query);
    b.putString("detailId", detailId);
    b.putString("storeId", storeId);
    b.putInt("scrollPosition", scroll == null ? 0 : scroll.getScrollY());
  }

  void load() {
    loadFailed = false;
    ready = false;
    final int generation = ++loadGeneration;
    render();
    new Thread(
            () -> {
              try {
                repository.load(this);
                handler.post(
                    () -> {
                      if (destroyed || generation != loadGeneration) return;
                      ready = true;
                      render();
                    });
              } catch (Exception e) {
                handler.post(
                    () -> {
                      if (destroyed || generation != loadGeneration) return;
                      loadFailed = true;
                      render();
                    });
              }
            },
            "offer-catalog")
        .start();
  }

  int dp(float x) {
    return Math.round(x * getResources().getDisplayMetrics().density);
  }

  GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  android.graphics.drawable.Drawable touch(int color, int radius) {
    return new RippleDrawable(
        ColorStateList.valueOf(0x18000000), bg(color, radius), bg(Color.WHITE, radius));
  }

  LinearLayout col() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  LinearLayout row() {
    LinearLayout v = new LinearLayout(this);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  void add(LinearLayout p, View v) {
    p.addView(v, new LinearLayout.LayoutParams(-1, -2));
  }

  void gap(LinearLayout p, int h) {
    View v = new View(this);
    p.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
  }

  TextView text(String s, int size, int color, boolean bold) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setFontFeatureSettings("kern");
    v.setIncludeFontPadding(false);
    v.setLineSpacing(dp(3), 1f);
    if (bold)
      v.setTypeface(
          android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    return v;
  }

  TextView button(String s, boolean primary, Runnable action) {
    TextView v = text(s, 15, primary ? Color.WHITE : INK, true);
    v.setGravity(Gravity.CENTER);
    v.setMinHeight(dp(50));
    v.setPadding(dp(16), dp(12), dp(16), dp(12));
    v.setBackground(touch(primary ? INK : SURFACE, 16));
    v.setOnClickListener(w -> action.run());
    v.setFocusable(true);
    return v;
  }

  void paragraph(String title, String body) {
    gap(content, 24);
    add(content, text(title, 16, INK, true));
    gap(content, 9);
    add(content, text(body, 15, MUTED, false));
  }

  void rule(LinearLayout p) {
    View v = new View(this);
    v.setBackgroundColor(LINE);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
    lp.topMargin = dp(18);
    lp.bottomMargin = dp(18);
    p.addView(v, lp);
  }

  void notice(String title, String body) {
    LinearLayout box = col();
    box.setPadding(dp(16), dp(16), dp(16), dp(16));
    box.setBackground(bg(SURFACE, 18));
    add(box, text(title, 15, INK, true));
    gap(box, 7);
    add(box, text(body, 13, MUTED, false));
    add(content, box);
  }

  void header(String title, String subtitle) {
    add(content, text(title, 28, INK, true));
    if (subtitle != null) {
      gap(content, 8);
      add(content, text(subtitle, 15, MUTED, false));
    }
    gap(content, 24);
  }

  void disposeMap() {
    if (map != null) {
      map.stopLoading();
      map.destroy();
      map = null;
    }
  }

  void render() {
    renderedVerification = verification.state();
    disposeMap();
    root = col();
    root.setBackgroundColor(Color.WHITE);
    root.setFitsSystemWindows(true);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, in) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars =
                in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          } else {
            v.setPadding(
                in.getSystemWindowInsetLeft(),
                in.getSystemWindowInsetTop(),
                in.getSystemWindowInsetRight(),
                in.getSystemWindowInsetBottom());
          }
          return in;
        });
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    content = col();
    content.setPadding(dp(22), dp(20), dp(22), dp(24));
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (!prefs.getBoolean("onboardingComplete", false)) {
      onboarding();
      animatePage();
      return;
    }
    if (!flow.isEmpty()) {
      identityFlow();
      animatePage();
      return;
    }
    if (detailId != null && ready) {
      Offer o = find(detailId);
      if (o != null) detail(o);
      else {
        detailId = null;
        home();
      }
    } else if (storeId != null && ready) {
      storeDetail(repository.stores.get(storeId));
    } else if (tab.equals("发现")) home();
    else if (tab.equals("附近")) nearby();
    else if (tab.equals("钱包")) wallet();
    else profile();
    if (detailId == null && storeId == null) navigation();
    animatePage();
    if (scrollPosition > 0 && ready) {
      int pos = scrollPosition;
      scrollPosition = 0;
      scroll.post(() -> scroll.scrollTo(0, pos));
    }
  }

  void animatePage() {
    if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
      content.setAlpha(0f);
      content.animate().alpha(1f).setDuration(160).start();
    }
  }

  Offer find(String id) {
    for (Offer o : repository.offers) if (o.id.equals(id)) return o;
    return null;
  }

  void navigation() {
    nav = row();
    nav.setPadding(dp(8), dp(8), dp(8), dp(10));
    for (String name : new String[] {"发现", "附近", "钱包", "我的"}) {
      LinearLayout item = col();
      item.setGravity(Gravity.CENTER);
      item.setPadding(dp(5), dp(6), dp(5), dp(6));
      boolean selected = tab.equals(name);
      item.setBackground(touch(selected ? SURFACE : Color.WHITE, 16));
      Icon icon = new Icon(name, selected ? INK : MUTED);
      item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
      gap(item, 4);
      TextView label = text(name, 12, selected ? INK : MUTED, selected);
      label.setGravity(Gravity.CENTER);
      item.addView(label);
      item.setContentDescription(name);
      item.setFocusable(true);
      item.setOnClickListener(
          v -> {
            if (tab.equals(name)) return;
            hideKeyboard();
            tab = name;
            query = "";
            category = "全部";
            render();
          });
      nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }
    add(root, nav);
  }

  void hideKeyboard() {
    View v = getCurrentFocus();
    if (v != null)
      ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(v.getWindowToken(), 0);
  }

  void home() {
    LinearLayout top = row();
    TextView brand = text("VaStudents", 20, INK, true);
    TextView cityButton = button(city + "  ⌄", false, this::chooseCity);
    if (getResources().getConfiguration().fontScale >= 1.4f) {
      top.setOrientation(LinearLayout.VERTICAL);
      add(top, brand);
      gap(top, 8);
      top.addView(cityButton);
    } else {
      top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
      top.addView(cityButton);
    }
    add(content, top);
    gap(content, 16);
    add(content, text("发现学生优惠", 24, INK, true));
    gap(content, 7);
    add(content, text("公开信息整理 · 使用前请核对官方条件", 14, MUTED, false));
    gap(content, 18);
    LinearLayout search = row();
    search.setPadding(dp(14), dp(4), dp(14), dp(4));
    search.setBackground(bg(SURFACE, 18));
    search.addView(new Icon("搜索", MUTED), new LinearLayout.LayoutParams(dp(22), dp(22)));
    EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setMinHeight(dp(44));
    input.setTextSize(15);
    input.setTextColor(INK);
    input.setHintTextColor(MUTED);
    input.setHint("搜索品牌或优惠");
    input.setContentDescription("搜索品牌或优惠");
    input.setBackgroundColor(Color.TRANSPARENT);
    input.setPadding(dp(10), dp(10), 0, dp(10));
    input.setText(query);
    search.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
    add(content, search);
    gap(content, 18);
    HorizontalScrollView hs = new HorizontalScrollView(this);
    hs.setHorizontalScrollBarEnabled(false);
    LinearLayout filters = row();
    filters.setPadding(dp(4), dp(4), dp(4), dp(4));
    filters.setBackground(bg(SURFACE, 24));
    for (String c : new String[] {"全部", "数码", "学习工具", "影音"}) {
      TextView f = text(c, 14, category.equals(c) ? INK : MUTED, category.equals(c));
      f.setGravity(Gravity.CENTER);
      f.setPadding(dp(16), dp(10), dp(16), dp(10));
      f.setMinHeight(dp(44));
      f.setBackground(touch(category.equals(c) ? Color.WHITE : SURFACE, 20));
      f.setOnClickListener(
          v -> {
            category = c;
            hideKeyboard();
            render();
          });
      filters.addView(f);
    }
    hs.addView(filters);
    add(content, hs);
    gap(content, 24);
    list = col();
    add(content, list);
    renderOffers(false);
    input.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            query = s.toString();
            renderOffers(false);
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  void renderOffers(boolean saved) {
    list.removeAllViews();
    if (!ready) {
      add(list, text(loadFailed ? "优惠暂时无法读取" : "正在读取优惠…", 17, INK, true));
      gap(list, 12);
      if (loadFailed) add(list, button("重新加载", true, this::load));
      else add(list, new ProgressBar(this));
      return;
    }
    ArrayList<Offer> matches = new ArrayList<>();
    for (Offer o : repository.offers)
      if (!o.isTestData && o.matches(query, category) && (!saved || favorites.contains(o.id)))
        matches.add(o);
    add(
        list,
        text(
            saved ? "我的收藏 · " + matches.size() : "精选优惠 · " + matches.size() + " 项", 16, INK, true));
    gap(list, 4);
    if (matches.isEmpty()) {
      gap(list, 30);
      add(list, text(saved ? "还没有收藏" : "没有找到匹配的优惠", 20, INK, true));
      gap(list, 10);
      add(list, text(saved ? "在优惠详情中收藏，下次更容易找到。" : "试试其他品牌名称，或切换到全部分类。", 14, MUTED, false));
      return;
    }
    for (Offer o : matches) {
      gap(list, 16);
      LinearLayout item = col();
      item.setPadding(dp(16), dp(16), dp(16), dp(16));
      item.setBackground(touch(SURFACE, 22));
      LinearLayout label = row();
      TextView initial = text(o.merchant.substring(0, 1), 21, INK, true);
      initial.setGravity(Gravity.CENTER);
      initial.setBackground(bg(Color.WHITE, 13));
      label.addView(initial, new LinearLayout.LayoutParams(dp(42), dp(42)));
      LinearLayout name = col();
      name.setPadding(dp(12), 0, dp(4), 0);
      add(name, text(o.merchant, 15, INK, true));
      gap(name, 3);
      add(
          name,
          text(
              o.category + (o.channel.equals("online") ? " · 线上使用" : " · 指定门店"), 12, MUTED, false));
      label.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
      label.addView(new Icon("箭头", MUTED), new LinearLayout.LayoutParams(dp(20), dp(20)));
      add(item, label);
      gap(item, 12);
      add(item, text(o.title, 19, INK, true));
      gap(item, 8);
      add(item, text(o.summary, 15, MUTED, false));
      gap(item, 10);
      add(item, text(o.benefit, 13, INK, true));
      gap(item, 8);
      add(item, text(o.freshness(LocalDate.now()) + " · " + o.verifiedAt, 12, MUTED, false));
      item.setContentDescription(o.title);
      item.setFocusable(true);
      item.setOnClickListener(
          v -> {
            hideKeyboard();
            scrollPosition = 0;
            detailId = o.id;
            render();
          });
      add(list, item);
    }
    gap(list, 20);
    add(list, text("独立信息聚合，不代表与品牌存在合作。", 12, MUTED, false));
  }

  void backButton() {
    TextView back = button("‹  返回", false, this::goBack);
    LinearLayout r = row();
    r.addView(back);
    add(content, r);
    gap(content, 24);
  }

  void detail(Offer o) {
    backButton();
    add(content, text(o.merchant + "  /  " + o.category, 14, MUTED, false));
    gap(content, 13);
    header(o.title, o.benefit);
    notice(o.freshness(LocalDate.now()), "最近核实 " + o.verifiedAt + " · 核对公开页面，不代表资格审核或领取成功。");
    paragraph("适用条件", o.eligibility);
    paragraph("有效期", o.validity);
    paragraph(
        "如何使用",
        o.channel.equals("online")
            ? "前往官方页面，按品牌要求完成资格验证和领取。VaStudents 不代为认证或收款。"
            : "仅适用于下方有明确来源证据的门店，请到店前核对。");
    paragraph("使用前请留意", o.limits);
    paragraph("信息来源", o.sourceUrl);
    gap(content, 12);
    add(content, button("查看官方来源", false, () -> external(o.sourceUrl)));
    if (!o.storeIds.isEmpty()) {
      paragraph("适用门店", "仅列出已核实的具体门店，不代表该品牌全部门店参与。");
      for (String id : o.storeIds) {
        OfferRepository.Store s = repository.stores.get(id);
        if (s != null)
          add(
              content,
              button(
                  s.name,
                  false,
                  () -> {
                    detailId = null;
                    storeId = id;
                    render();
                  }));
      }
    }
    gap(content, 26);
    TextView primary =
        button(
            o.actionable(LocalDate.now()) ? "前往官方领取 / 了解" : "前往官方核对当前状态",
            true,
            () -> external(o.sourceUrl));
    add(content, primary);
    gap(content, 10);
    TextView save = button(favorites.contains(o.id) ? "已收藏 · 取消收藏" : "收藏这项优惠", false, () -> {});
    save.setOnClickListener(
        v -> {
          if (!favorites.add(o.id)) favorites.remove(o.id);
          prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
          save.setText(favorites.contains(o.id) ? "已收藏 · 取消收藏" : "收藏这项优惠");
        });
    add(content, save);
  }

  void chooseCity() {
    new AlertDialog.Builder(this)
        .setTitle("选择城市")
        .setItems(
            cities,
            (d, which) -> {
              city = cities[which];
              prefs.edit().putString("city", city).apply();
              render();
            })
        .setNegativeButton("取消", null)
        .show();
  }

  void nearby() {
    header("附近", "手动选城，不读取你的定位");
    add(content, button(city + "  ·  切换城市", false, this::chooseCity));
    gap(content, 16);
    FrameLayout frame = new FrameLayout(this);
    frame.setBackground(bg(SURFACE, 20));
    frame.setClipToOutline(true);
    add(content, frame);
    frame.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(300)));
    TextView status = text("正在加载真实地图…", 13, MUTED, false);
    status.setPadding(dp(12), dp(8), dp(12), dp(8));
    status.setBackgroundColor(Color.WHITE);
    map = new WebView(this);
    WebView current = map;
    map.getSettings().setJavaScriptEnabled(true);
    map.getSettings().setDomStorageEnabled(false);
    map.getSettings().setAllowFileAccess(false);
    map.getSettings().setAllowContentAccess(false);
    map.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
    map.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    map.getSettings()
        .setUserAgentString(map.getSettings().getUserAgentString() + " VaStudents/0.1.0");
    map.setContentDescription("OpenStreetMap 交互地图");
    frame.addView(map, new FrameLayout.LayoutParams(-1, -1));
    FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
    content.addView(status, content.indexOfChild(frame), new LinearLayout.LayoutParams(-1, -2));
    map.setOnTouchListener(
        (v, e) -> {
          scroll.requestDisallowInterceptTouchEvent(
              e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL);
          return false;
        });
    map.setWebViewClient(
        new WebViewClient() {
          public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            if (r.isForMainFrame()) {
              external(r.getUrl().toString());
              return true;
            }
            return false;
          }

          public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
            if (r.isForMainFrame()) status.setText("地图加载失败，可重试或打开外部地图");
          }

          public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse e) {
            if (r.isForMainFrame()) status.setText("地图服务暂时不可用");
          }

          public void onPageFinished(WebView v, String url) {
            checkTiles(current, status, 0);
          }
        });
    double[] c = centre();
    String bbox = (c[0] - .045) + "," + (c[1] - .027) + "," + (c[0] + .045) + "," + (c[1] + .027);
    map.loadUrl("https://www.openstreetmap.org/export/embed.html?bbox=" + bbox + "&layer=mapnik");
    gap(content, 8);
    TextView attribution = text("© OpenStreetMap contributors", 12, MUTED, false);
    attribution.setMinHeight(dp(40));
    attribution.setGravity(Gravity.CENTER_VERTICAL);
    attribution.setOnClickListener(v -> external("https://www.openstreetmap.org/copyright"));
    add(content, attribution);
    add(
        content,
        button(
            "重新加载地图",
            false,
            () -> {
              if (map != null) map.reload();
            }));
    gap(content, 12);
    notice(
        repository.stores.isEmpty() ? "暂无已核实的参与门店" : "参与门店以核实列表为准",
        "地图中的地点不代表参与学生优惠。收录具体门店前，需要可追溯的适用范围证据。");
    gap(content, 20);
    for (OfferRepository.Store s : repository.stores.values()) {
      boolean participates = false;
      for (Offer o : repository.offers)
        if (!o.isTestData && o.actionable(LocalDate.now()) && o.appliesTo(s.id))
          participates = true;
      if (s.city.equals(city) && participates)
        add(
            content,
            button(
                s.name,
                false,
                () -> {
                  storeId = s.id;
                  render();
                }));
    }
    add(
        content,
        button(
            "在外部地图查看城市",
            true,
            () -> external("https://www.openstreetmap.org/#map=13/" + c[1] + "/" + c[0])));
    gap(content, 12);
    add(
        content,
        text("地图仅在打开此页时联网，服务方可收到 IP 和地图区域。使用 WGS84 坐标；未启用定位。公共地图服务可能暂时不可用。", 12, MUTED, false));
  }

  void checkTiles(WebView current, TextView status, int attempt) {
    handler.postDelayed(
        () -> {
          if (map != current || destroyed) return;
          current.evaluateJavascript(
              "Array.from(document.images).filter(i=>i.classList.contains('leaflet-tile')&&i.complete&&i.naturalWidth>0).length+(document.querySelector('canvas')?performance.getEntriesByType('resource').filter(r=>r.name.startsWith('https://tile.openstreetmap.org/')&&r.responseStatus===200).length:0)",
              value -> {
                if (map != current) return;
                try {
                  if (Integer.parseInt(value) > 0) {
                    status.setText("地图已加载 · " + city + " · 非参与门店标记");
                    return;
                  }
                } catch (Exception ignored) {
                }
                if (attempt < 7) checkTiles(current, status, attempt + 1);
                else status.setText("未确认地图瓦片加载，请重试或使用外部地图");
              });
        },
        1500);
  }

  double[] centre() {
    for (int i = 0; i < cities.length; i++) if (cities[i].equals(city)) return centres[i];
    return centres[0];
  }

  void storeDetail(OfferRepository.Store s) {
    backButton();
    if (s == null) {
      header("门店资料暂不可用", "请返回后重试");
      return;
    }
    header(s.name, s.city);
    notice("已核实的具体门店", "地址与适用优惠需以来源为准；营业时间未核实。");
    paragraph("地址", s.address);
    paragraph("最近核实", s.verifiedAt);
    gap(content, 20);
    add(content, button("查看门店来源", false, () -> external(s.sourceUrl)));
    gap(content, 12);
    add(
        content,
        button(
            "前往目的地导航",
            true,
            () ->
                external(
                    "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=;"
                        + s.latitude
                        + ","
                        + s.longitude)));
  }

  void wallet() {
    header("ID Wallet", "学生身份与权益，一处查看");
    LinearLayout card = col();
    card.setPadding(dp(22), dp(22), dp(22), dp(22));
    card.setBackground(bg(SURFACE, 24));
    add(card, text("VaStudents  /  学生 ID", 14, MUTED, false));
    gap(card, 28);
    add(card, text(verification.label(), 23, INK, true));
    gap(card, 10);
    add(
        card,
        text(verification.loggedIn() ? "测试账户 · 测试大学（虚构）" : "可先浏览公开优惠，需要时再验证", 14, MUTED, false));
    gap(card, 28);
    add(card, text("不可作为真实学生证明", 12, MUTED, false));
    add(content, card);
    gap(content, 22);
    add(content, button("绑定与学生验证", true, () -> openIdentity("wallet")));
    gap(content, 10);
    add(
        content,
        button(
            BuildConfig.DEVELOPMENT_FEATURES ? "学生验证二维码" : "学生验证二维码 · 尚未开放",
            false,
            () -> openIdentity("qr")));
    paragraph("可用权益", "当前没有由 VaStudents 发放的商户权益。公开优惠需到官方渠道验证和领取。");
    rule(content);
    add(content, text("核销码", 19, INK, true));
    gap(content, 8);
    add(content, text("尚无合作商家接受 VaStudents 核销码。下面仅用于体验流程，不可消费或兑换。", 14, MUTED, false));
    gap(content, 15);
    add(
        content,
        button(BuildConfig.DEVELOPMENT_FEATURES ? "查看核销码演示" : "核销服务 · 尚未开放", false, this::demo));
    paragraph("真实核销记录", "暂无记录。演示不会写入真实核销记录。");
  }

  void demo() {
    if (!BuildConfig.DEVELOPMENT_FEATURES) {
      new AlertDialog.Builder(this)
          .setTitle("核销服务尚未开放")
          .setMessage("暂无合作商户或有效权益，不生成核销码。")
          .setPositiveButton("知道了", null)
          .show();
      return;
    }
    long now = System.currentTimeMillis();
    String code = wallet.demoCode(now);
    LinearLayout box = col();
    box.setPadding(dp(24), dp(20), dp(24), dp(20));
    add(box, text("仅测试 · 商户不可核销", 16, INK, true));
    gap(box, 18);
    add(box, text(code, 24, INK, true));
    gap(box, 16);
    TextView countdown = text("", 14, MUTED, false);
    add(box, countdown);
    gap(box, 12);
    add(box, text("不代表已验证学生身份、有效权益或真实核销结果。", 14, MUTED, false));
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("核销流程演示")
            .setView(box)
            .setPositiveButton("关闭", null)
            .create();
    dialog.show();
    Runnable tick =
        new Runnable() {
          public void run() {
            if (!dialog.isShowing()) return;
            long seconds = wallet.remainingSeconds(System.currentTimeMillis());
            countdown.setText(seconds > 0 ? "演示码剩余 " + seconds + " 秒" : "演示码已过期，请关闭后重新打开");
            if (seconds > 0) handler.postDelayed(this, 1000);
          }
        };
    tick.run();
    dialog.setOnDismissListener(d -> handler.removeCallbacks(tick));
  }

  void profile() {
    header("我的", "偏好与收藏只保存在这台设备");
    LinearLayout local = row();
    local.addView(new Icon("我的", INK), new LinearLayout.LayoutParams(dp(38), dp(38)));
    LinearLayout label = col();
    label.setPadding(dp(14), 0, 0, 0);
    add(label, text(verification.loggedIn() ? "测试账户（开发演示）" : "本地访客", 20, INK, true));
    gap(label, 6);
    add(label, text("无需登录也能浏览公开优惠", 13, MUTED, false));
    local.addView(label);
    add(content, local);
    gap(content, 20);
    add(
        content,
        button(
            verification.loggedIn() ? "管理测试学生身份" : "登录 / 绑定学生身份",
            true,
            () -> openIdentity("wallet")));
    gap(content, 26);
    list = col();
    add(content, list);
    renderOffers(true);
    rule(content);
    add(content, button("城市偏好 · " + city, false, this::chooseCity));
    gap(content, 12);
    add(
        content,
        button(
            "数据与隐私说明",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("数据与隐私")
                    .setMessage(
                        "收藏与城市保存在本机。未申请定位、相机、通讯录权限，未收集身份材料。附近地图由 OpenStreetMap 提供，打开地图时会向服务方发送 IP"
                            + " 和地图区域。官方优惠与导航跳转由外部服务处理。当前无账户同步、广告或分析 SDK。")
                    .setPositiveButton("知道了", null)
                    .show()));
    gap(content, 12);
    add(
        content,
        button(
            "清除本地收藏和城市偏好",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("清除本地偏好？")
                    .setMessage("将清除本应用的收藏和已选城市。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton(
                        "清除",
                        (d, w) -> {
                          prefs.edit().remove("favorites").remove("city").apply();
                          favorites.clear();
                          city = "长沙";
                          render();
                        })
                    .show()));
    gap(content, 22);
    add(content, text("VaStudents 0.1.0 · 开发预览\n独立学生优惠信息聚合，无品牌合作背书。", 12, MUTED, false));
  }

  void onboarding() {
    gap(content, 28);
    TextView mark = text("V", 38, Color.WHITE, true);
    mark.setGravity(Gravity.CENTER);
    mark.setBackground(bg(INK, 24));
    content.addView(mark, new LinearLayout.LayoutParams(dp(82), dp(82)));
    gap(content, 28);
    header("欢迎来到 VaStudents", "先发现值得了解的学生优惠");
    notice("先看看，也很好", "无需登录或认证，就能浏览公开优惠、查看条件和收藏。");
    paragraph("需要资格时，再验证", "公开优惠按品牌自己的规则领取。VaStudents 身份不替代品牌认证，也不代表商户合作。");
    paragraph("你决定提供什么", "当前不收集真实学生材料或定位。地图在打开时联网，收藏保存在本机。");
    gap(content, 28);
    add(
        content,
        button(
            "开始浏览",
            true,
            () -> {
              prefs.edit().putBoolean("onboardingComplete", true).apply();
              render();
            }));
    gap(content, 12);
    add(
        content,
        button(
            "了解身份与隐私",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("身份与隐私")
                    .setMessage(
                        "真实登录、材料审核和商户核销服务尚未开放。开发版本中的测试身份始终带演示标识，不能当作有效学生证明。地图服务可能接收 IP"
                            + " 与所选区域，不读取精确定位。")
                    .setPositiveButton("知道了", null)
                    .show()));
    gap(content, 22);
    add(
        content,
        text(
            BuildConfig.DEVELOPMENT_FEATURES ? "开发演示版 · 仅合成测试数据" : "体验预览版 · 真实认证尚未开放",
            12,
            MUTED,
            false));
  }

  void openIdentity(String intent) {
    returnIntent = intent;
    flow = verification.loggedIn() ? "verification" : "account";
    render();
  }

  void identityFlow() {
    backButton();
    if (flow.equals("account")) {
      header("连接你的学生身份", "浏览公开优惠不需要登录");
      notice("真实账户服务尚未开放", "未来登录后可绑定学生身份。当前不收集手机号、学校邮箱或身份证件。");
      paragraph("身份与权益分开管理", "登录不等于在读资格已通过，材料提交也不等于真实性已确认。具体优惠仍以官方条件为准。");
      gap(content, 26);
      if (BuildConfig.DEVELOPMENT_FEATURES) {
        add(
            content,
            button(
                "使用测试账户 · 开发演示",
                true,
                () -> {
                  verification.login();
                  flow = "verification";
                  render();
                }));
        gap(content, 12);
        add(content, text("仅创建本机虚构测试会话，不是实际注册或登录。", 12, MUTED, false));
      } else
        add(
            content,
            button(
                "查看尚未开放的服务",
                true,
                () ->
                    new AlertDialog.Builder(this)
                        .setTitle("正在准备身份服务")
                        .setMessage("真实账户认证、审核主体、上传存储及材料保留策略尚待配置和确认。当前无法提交真实材料。")
                        .setPositiveButton("知道了", null)
                        .show()));
      gap(content, 16);
      add(
          content,
          button(
              "继续浏览公开优惠",
              false,
              () -> {
                flow = "";
                returnIntent = "";
                tab = "发现";
                render();
              }));
      return;
    }
    if (flow.equals("qr")) {
      verificationQr();
      return;
    }
    header("学生身份验证", verification.label());
    if (!BuildConfig.DEVELOPMENT_FEATURES) {
      notice("真实认证尚未开放", "本体验版本不提供开发跳过，也不接受真实材料。");
      return;
    }
    notice("全程仅开发演示", "测试大学（虚构） · 合成材料。未连接学信网，也没有完成任何权威核验。");
    DevelopmentVerification.State state = verification.state();
    paragraph("分别确认三件事", "本人身份：未进行真实核验\n在读资格：仅演示审核状态\n材料真实性：没有权威接口校验");
    if (state == DevelopmentVerification.State.NONE
        || state == DevelopmentVerification.State.REJECTED
        || state == DevelopmentVerification.State.EXPIRED
        || state == DevelopmentVerification.State.REVOKED) {
      paragraph("准备测试材料", "只生成一份明确标记 SYNTHETIC TEST 的文本，不打开文件选择器，不读取或上传真实学信网材料。");
      paragraph(
          "存储与删除",
          "测试文件位于本应用私有且不备份的目录，7 天后在下次启动时清除到期文件，未启动期间可能继续留存在本机；也可以立即删除。真实原件的收集范围和保留时间另需确认。");
      gap(content, 24);
      add(
          content,
          button(
              state == DevelopmentVerification.State.NONE ? "创建并提交合成测试材料" : "重新提交合成测试材料",
              true,
              () -> {
                try {
                  verification.submit();
                  render();
                } catch (Exception e) {
                  new AlertDialog.Builder(this)
                      .setTitle("测试材料保存失败")
                      .setMessage("未提交，请重试。未读取真实文件。")
                      .setPositiveButton("知道了", null)
                      .show();
                }
              }));
    } else if (state == DevelopmentVerification.State.PENDING) {
      paragraph("已提交，等待审核", "这是本机状态机演示，没有向远程审核机构提交。重复点击不会创建多份申请。");
      gap(content, 24);
      add(
          content,
          button(
              "模拟审核结果 · 仅开发",
              true,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("选择演示结果")
                      .setItems(
                          new String[] {"测试通过（非真实认证）", "测试拒绝"},
                          (d, w) -> {
                            safeReview(
                                w == 0
                                    ? DevelopmentVerification.State.APPROVED
                                    : DevelopmentVerification.State.REJECTED);
                          })
                      .show()));
    } else if (state == DevelopmentVerification.State.APPROVED) {
      paragraph("演示通过，不代表真实身份", "测试状态有效期 24 小时；实际学生资格仍未验证，没有可核销商户权益。");
      gap(content, 24);
      add(
          content,
          button(
              returnIntent.equals("qr") ? "返回并查看测试二维码" : "返回 ID Wallet",
              true,
              () -> {
                if (returnIntent.equals("qr")) {
                  flow = "qr";
                } else {
                  flow = "";
                  tab = "钱包";
                }
                render();
              }));
      gap(content, 12);
      add(
          content,
          button(
              "测试到期 / 撤销",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("测试状态变化")
                      .setItems(
                          new String[] {"设为已到期", "设为已撤销"},
                          (d, w) -> {
                            safeReview(
                                w == 0
                                    ? DevelopmentVerification.State.EXPIRED
                                    : DevelopmentVerification.State.REVOKED);
                          })
                      .show()));
    }
    gap(content, 18);
    add(
        content,
        button(
            "删除测试材料与测试账户",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("删除本机测试数据？")
                    .setMessage("会删除合成材料、测试账户和审核演示状态，保留公开优惠收藏。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton(
                        "删除",
                        (d, w) -> {
                          verification.reset();
                          flow = "account";
                          render();
                        })
                    .show()));
  }

  void safeReview(DevelopmentVerification.State state) {
    try {
      verification.review(state);
    } catch (IllegalStateException e) {
      Toast.makeText(this, "状态已变化，请重新查看", Toast.LENGTH_SHORT).show();
    }
    render();
  }

  void verificationQr() {
    header("学生验证二维码", "仅测试 · 不是学生证明或商户核销码");
    if (!BuildConfig.DEVELOPMENT_FEATURES
        || verification.state() != DevelopmentVerification.State.APPROVED) {
      notice("当前不可生成", "请先完成开发验证流程；真实认证服务尚未开放。");
      gap(content, 24);
      add(
          content,
          button(
              "返回验证流程",
              true,
              () -> {
                flow = "verification";
                render();
              }));
      return;
    }
    if (qrExpiresAt > 0 && System.currentTimeMillis() >= qrExpiresAt) {
      notice("测试二维码已过期", "请主动生成新的测试码。旧码不可继续展示。");
      gap(content, 24);
      add(
          content,
          button(
              "生成新的测试二维码",
              true,
              () -> {
                qrExpiresAt = 0;
                render();
              }));
      return;
    }
    if (qrExpiresAt == 0) {
      qrExpiresAt = System.currentTimeMillis() + 60000;
      qrPayload =
          "vastudents-test://student-id?status=synthetic-only&expires="
              + qrExpiresAt
              + "&nonce="
              + UUID.randomUUID();
    }
    final long expiry = qrExpiresAt;
    final String payload = qrPayload;
    try {
      com.google.zxing.common.BitMatrix matrix =
          new com.google.zxing.qrcode.QRCodeWriter()
              .encode(payload, com.google.zxing.BarcodeFormat.QR_CODE, 640, 640);
      Bitmap bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);
      for (int y = 0; y < 640; y++)
        for (int x = 0; x < 640; x++) bitmap.setPixel(x, y, matrix.get(x, y) ? INK : Color.WHITE);
      ImageView qr = new ImageView(this);
      qr.setImageBitmap(bitmap);
      qr.setContentDescription("测试学生二维码，不可用于真实认证或商户核销");
      add(content, qr);
      qr.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(260)));
      gap(content, 18);
      TextView timer = text("测试码有效期 60 秒", 16, INK, true);
      add(content, timer);
      handler.postDelayed(
          () -> {
            if (!destroyed && flow.equals("qr")) {
              qr.setImageDrawable(null);
              timer.setText("测试二维码已过期");
            }
          },
          Math.max(0, expiry - System.currentTimeMillis()));
    } catch (Exception e) {
      notice("二维码生成失败", "请返回后重试。");
    }
    paragraph("扫码后的边界", "二维码含测试标记和到期时间，不含姓名、学号或材料。尚无商户或真实验证端接受本码，也未建立防复制的服务端验证。");
    gap(content, 20);
    add(
        content,
        button(
            "返回 ID Wallet",
            true,
            () -> {
              flow = "";
              returnIntent = "";
              tab = "钱包";
              render();
            }));
  }

  void external(String url) {
    if (SystemClock.elapsedRealtime() - lastExternal < 800) return;
    lastExternal = SystemClock.elapsedRealtime();
    Uri uri = Uri.parse(url);
    if (!"https".equals(uri.getScheme())) return;
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, uri));
    } catch (ActivityNotFoundException e) {
      new AlertDialog.Builder(this)
          .setTitle("没有可打开链接的应用")
          .setMessage(url)
          .setPositiveButton("知道了", null)
          .show();
    }
  }

  void goBack() {
    if (!flow.isEmpty()) {
      flow = "";
      returnIntent = "";
      render();
    } else if (storeId != null) {
      storeId = null;
      render();
    } else if (detailId != null) {
      detailId = null;
      render();
    } else if (!tab.equals("发现")) {
      tab = "发现";
      render();
    } else finish();
  }

  @android.annotation.SuppressLint(
      "GestureBackNavigation") // API <=32 fallback; API33+ registers OnBackInvokedCallback.
  @Override
  public void onBackPressed() {
    goBack();
  }

  @Override
  protected void onPause() {
    if (map != null) map.onPause();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (verification != null
        && (verification.state() != renderedVerification
            || (flow.equals("qr") && qrExpiresAt > 0 && System.currentTimeMillis() >= qrExpiresAt)))
      render();
    if (map != null) map.onResume();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    handler.removeCallbacksAndMessages(null);
    disposeMap();
    super.onDestroy();
  }

  class Icon extends View {
    String name;
    Paint p = new Paint(3);
    Path path = new Path();

    Icon(String n, int color) {
      super(MainActivity.this);
      name = n;
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.save();
      canvas.scale(getWidth() / 24f, getHeight() / 24f);
      path.reset();
      switch (name) {
        case "发现":
          path.moveTo(4, 7);
          path.lineTo(12, 3);
          path.lineTo(20, 7);
          path.lineTo(20, 20);
          path.lineTo(4, 20);
          path.close();
          canvas.drawPath(path, p);
          canvas.drawLine(9, 20, 9, 13, p);
          canvas.drawLine(15, 20, 15, 13, p);
          canvas.drawLine(9, 13, 15, 13, p);
          break;
        case "附近":
          canvas.drawCircle(12, 10, 3, p);
          path.moveTo(19, 10);
          path.cubicTo(19, 15, 12, 22, 12, 22);
          path.cubicTo(12, 22, 5, 15, 5, 10);
          path.cubicTo(5, 1, 19, 1, 19, 10);
          canvas.drawPath(path, p);
          break;
        case "钱包":
          canvas.drawRoundRect(3, 5, 21, 20, 3, 3, p);
          canvas.drawRoundRect(14, 10, 22, 15, 2, 2, p);
          canvas.drawPoint(17, 12.5f, p);
          break;
        case "我的":
          canvas.drawCircle(12, 7, 4, p);
          path.moveTo(4, 21);
          path.cubicTo(4, 11, 20, 11, 20, 21);
          canvas.drawPath(path, p);
          break;
        case "搜索":
          canvas.drawCircle(10, 10, 6, p);
          canvas.drawLine(15, 15, 21, 21, p);
          break;
        default:
          path.moveTo(9, 5);
          path.lineTo(16, 12);
          path.lineTo(9, 19);
          canvas.drawPath(path, p);
      }
      canvas.restore();
    }
  }
}
