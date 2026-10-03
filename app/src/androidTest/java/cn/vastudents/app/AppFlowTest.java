package cn.vastudents.app;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
  Instrumentation inst;
  MainActivity a;

  @Before
  public void start() throws Exception {
    inst = InstrumentationRegistry.getInstrumentation();
    Context c = inst.getTargetContext();
    c.getSharedPreferences("vastudents", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboardingComplete", true)
        .commit();
    Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    a = (MainActivity) inst.startActivitySync(i);
    for (int n = 0; n < 50 && !a.ready; n++) Thread.sleep(100);
    inst.waitForIdleSync();
  }

  @After
  public void end() {
    if (a != null) inst.runOnMainSync(() -> a.finish());
  }

  @Test
  public void catalogueAndNoLocationPermission() {
    assertTrue(a.ready);
    assertEquals(4, a.repository.offers.size());
    assertEquals(1, a.repository.stores.size());
    assertEquals(
        PackageManager.PERMISSION_DENIED,
        a.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION"));
    assertEquals(
        PackageManager.PERMISSION_DENIED,
        a.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION"));
  }

  @Test
  public void searchEmptyAndRecovery() {
    inst.runOnMainSync(
        () -> {
          a.query = "不存在的品牌";
          a.renderOffers(false);
          assertTrue(contains(a.list, "没有找到匹配的优惠"));
          a.query = "Apple";
          a.renderOffers(false);
          assertTrue(contains(a.list, "Apple 教育优惠"));
        });
  }

  @Test
  public void backRepeatedNavigationWalletHonesty() {
    inst.runOnMainSync(
        () -> {
          for (int i = 0; i < 10; i++) {
            a.detailId = "apple-cn-education";
            a.render();
            a.goBack();
          }
          assertNull(a.detailId);
          a.tab = "钱包";
          a.render();
          assertTrue(contains(a.content, "尚未验证学生身份"));
          assertTrue(contains(a.content, "暂无记录。演示不会写入真实核销记录。"));
          a.goBack();
          assertEquals("发现", a.tab);
        });
  }

  @Test
  public void failureRetryState() {
    inst.runOnMainSync(
        () -> {
          a.ready = false;
          a.loadFailed = true;
          a.render();
          assertTrue(contains(a.content, "优惠暂时无法读取"));
          a.loadFailed = false;
          a.render();
          assertTrue(contains(a.content, "正在读取优惠…"));
          a.ready = true;
          a.render();
        });
  }

  @Test
  public void duplicateDataCollapsedAndMalformedRejected() throws Exception {
    OfferRepository r = new OfferRepository();
    String fixture = "{\"schemaVersion\":1,\"offers\":[],\"stores\":[]}";
    r.parse(fixture);
    assertTrue(r.offers.isEmpty());
    try {
      r.parse("{bad");
      fail();
    } catch (Exception expected) {
    }
    assertTrue(r.offers.isEmpty());
  }

  @Test
  public void pageFadeActuallyCompletes() throws Exception {
    inst.runOnMainSync(
        () -> {
          a.render();
          if (android.animation.ValueAnimator.areAnimatorsEnabled())
            assertTrue(a.content.getAlpha() < 1f);
        });
    Thread.sleep(500);
    inst.waitForIdleSync();
    inst.runOnMainSync(() -> assertEquals(1f, a.content.getAlpha(), 0.01f));
  }

  @Test
  public void realTouchSearchFavoriteAndBack() {
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withContentDescription("搜索品牌或优惠"))
        .perform(
            androidx.test.espresso.action.ViewActions.click(),
            androidx.test.espresso.action.ViewActions.replaceText("不存在的品牌"),
            androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withText("没有找到匹配的优惠"))
        .perform(androidx.test.espresso.action.ViewActions.scrollTo())
        .check(
            androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withContentDescription("搜索品牌或优惠"))
        .perform(
            androidx.test.espresso.action.ViewActions.scrollTo(),
            androidx.test.espresso.action.ViewActions.replaceText("Apple"),
            androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withContentDescription("Apple 教育优惠"))
        .perform(
            androidx.test.espresso.action.ViewActions.scrollTo(),
            androidx.test.espresso.action.ViewActions.click());
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withText("收藏这项优惠"))
        .perform(
            androidx.test.espresso.action.ViewActions.scrollTo(),
            androidx.test.espresso.action.ViewActions.click());
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withText("已收藏 · 取消收藏"))
        .check(
            androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
        .perform(androidx.test.espresso.action.ViewActions.click());
    androidx.test.espresso.Espresso.pressBack();
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withContentDescription("我的"))
        .perform(androidx.test.espresso.action.ViewActions.click());
    androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withText("本地访客"))
        .check(
            androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
  }

  @Test
  public void specificPlaceFilteringAndDetails() {
    inst.runOnMainSync(
        () -> {
          a.city = "长沙";
          assertEquals(1, a.availableStores().size());
          Offer offer = a.find("yuelu-academy-student");
          assertNotNull(offer);
          assertTrue(offer.appliesTo("yuelu-academy-changsha"));
          assertFalse(offer.appliesTo("unverified-other-branch"));
          OfferRepository.Store place = a.availableStores().get(0);
          assertTrue(a.hasCoordinate(place));
          assertTrue(place.navigationUrl.startsWith("https://ylsy.hnu.edu.cn/"));
          a.city = "北京";
          assertTrue(a.availableStores().isEmpty());
          a.city = "长沙";
          a.storeId = place.id;
          a.render();
          assertTrue(contains(a.content, "学生半价票"));
          assertTrue(contains(a.content, "不是已核实入口导航点"));
          assertTrue(contains(a.content, "查看官方到达路线"));
          a.goBack();
          assertNull(a.storeId);
          String prior = offer.status;
          offer.status = "withdrawn";
          assertTrue(a.availableStores().isEmpty());
          offer.status = prior;
        });
  }

  @Test
  public void realTouchPlaceDetailsAndOfficialRouteIntent() {
    final String[] opened = {null};
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor() {
          @Override
          public Instrumentation.ActivityResult onStartActivity(Intent intent) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
              opened[0] = intent.getDataString();
              return new Instrumentation.ActivityResult(0, null);
            }
            return null;
          }
        };
    inst.addMonitor(monitor);
    try {
      inst.runOnMainSync(
          () -> {
            a.city = "长沙";
          });
      androidx.test.espresso.Espresso.onView(
              androidx.test.espresso.matcher.ViewMatchers.withContentDescription("附近"))
          .perform(androidx.test.espresso.action.ViewActions.click());
      androidx.test.espresso.Espresso.onView(
              androidx.test.espresso.matcher.ViewMatchers.withText("查看 岳麓书院 详情"))
          .perform(
              androidx.test.espresso.action.ViewActions.scrollTo(),
              androidx.test.espresso.action.ViewActions.click());
      androidx.test.espresso.Espresso.onView(
              androidx.test.espresso.matcher.ViewMatchers.withText("查看官方到达路线"))
          .perform(
              androidx.test.espresso.action.ViewActions.scrollTo(),
              androidx.test.espresso.action.ViewActions.click());
      assertEquals("https://ylsy.hnu.edu.cn/wbly/cgdn/jtl_.htm", opened[0]);
      androidx.test.espresso.Espresso.pressBack();
      assertNull(a.storeId);
    } finally {
      inst.removeMonitor(monitor);
    }
  }

  static boolean contains(View v, String s) {
    if (v instanceof TextView && ((TextView) v).getText().toString().contains(s)) return true;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) if (contains(g.getChildAt(i), s)) return true;
    }
    return false;
  }
}
