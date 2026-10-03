package cn.vastudents.app;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IdentityFlowTest {
  Instrumentation inst;
  MainActivity a;

  @Before
  public void start() throws Exception {
    inst = InstrumentationRegistry.getInstrumentation();
    Context c = inst.getTargetContext();
    c.getSharedPreferences("vastudents", 0).edit().putBoolean("onboardingComplete", true).commit();
    a =
        (MainActivity)
            inst.startActivitySync(
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    for (int n = 0; n < 50 && !a.ready; n++) Thread.sleep(100);
    inst.waitForIdleSync();
  }

  @After
  public void end() {
    inst.runOnMainSync(() -> a.finish());
  }

  @Test
  public void previewCannotInvokeDevelopmentActions() {
    if (BuildConfig.DEVELOPMENT_FEATURES) return;
    inst.runOnMainSync(
        () -> {
          assertFalse(a.verification.loggedIn());
          try {
            a.verification.login();
            fail("Preview accepted test login");
          } catch (SecurityException expected) {
          }
          try {
            a.verification.submit();
            fail("Preview accepted test material");
          } catch (SecurityException expected) {
          } catch (IOException e) {
            fail();
          }
          try {
            a.verification.review(DevelopmentVerification.State.APPROVED);
            fail("Preview accepted test review");
          } catch (SecurityException expected) {
          }
          assertFalse(a.verification.isRealVerified());
        });
  }

  @Test
  public void completeSyntheticLifecycleAndReturnIntent() throws Exception {
    if (!BuildConfig.DEVELOPMENT_FEATURES) return;
    inst.runOnMainSync(
        () -> {
          a.verification.reset();
          a.openIdentity("qr");
          assertEquals("account", a.flow);
          a.verification.login();
          a.flow = "verification";
          try {
            a.verification.submit();
            a.verification.submit();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          assertEquals(DevelopmentVerification.State.PENDING, a.verification.state());
          a.verification.review(DevelopmentVerification.State.REJECTED);
          assertEquals(DevelopmentVerification.State.REJECTED, a.verification.state());
          try {
            a.verification.submit();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          a.verification.review(DevelopmentVerification.State.APPROVED);
          assertEquals("qr", a.returnIntent);
          assertFalse(a.verification.isRealVerified());
          a.flow = "qr";
          a.render();
          assertTrue(AppFlowTest.contains(a.content, "仅测试"));
          a.verification.review(DevelopmentVerification.State.REVOKED);
          assertEquals(DevelopmentVerification.State.REVOKED, a.verification.state());
          a.render();
          assertTrue(AppFlowTest.contains(a.content, "当前不可生成"));
          a.verification.reset();
          assertFalse(a.verification.material().exists());
        });
  }

  @Test
  public void screenshotsOfActualNativePages() throws Exception {
    inst.runOnMainSync(
        () -> {
          if (BuildConfig.DEVELOPMENT_FEATURES) a.verification.reset();
          a.prefs.edit().putBoolean("onboardingComplete", false).commit();
          a.render();
        });
    screen("01-welcome");
    inst.runOnMainSync(
        () -> {
          a.prefs.edit().putBoolean("onboardingComplete", true).commit();
          a.tab = "发现";
          a.render();
        });
    screen("02-home");
    inst.runOnMainSync(
        () -> {
          a.detailId = "apple-cn-education";
          a.render();
        });
    screen("03-offer-detail");
    inst.runOnMainSync(
        () -> {
          a.detailId = null;
          a.tab = "钱包";
          a.render();
        });
    screen("04-wallet");
    inst.runOnMainSync(() -> a.openIdentity("qr"));
    screen("05-account");
    if (BuildConfig.DEVELOPMENT_FEATURES) {
      inst.runOnMainSync(
          () -> {
            a.verification.login();
            a.flow = "verification";
            a.render();
          });
      screen("06-submit-test-material");
      inst.runOnMainSync(
          () -> {
            try {
              a.verification.submit();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            a.render();
          });
      screen("07-pending");
      inst.runOnMainSync(
          () -> {
            a.verification.review(DevelopmentVerification.State.APPROVED);
            a.render();
          });
      screen("08-test-approved");
      inst.runOnMainSync(
          () -> {
            a.flow = "qr";
            a.render();
          });
      screen("09-test-qr");
      inst.runOnMainSync(() -> a.verification.reset());
    }
    inst.runOnMainSync(
        () -> {
          a.flow = "";
          a.tab = "我的";
          a.render();
        });
    screen("10-profile");
    inst.runOnMainSync(
        () -> {
          a.storeId = "yuelu-academy-changsha";
          a.render();
        });
    screen("12-place-detail");
    inst.runOnMainSync(
        () -> {
          a.storeId = null;
          a.detailId = "yuelu-academy-student";
          a.render();
        });
    screen("13-offline-offer");
  }

  @Test
  public void actualQrDecodesAndExpiresWithoutAutoRefresh() {
    if (!BuildConfig.DEVELOPMENT_FEATURES) return;
    inst.runOnMainSync(
        () -> {
          a.verification.reset();
          a.verification.login();
          try {
            a.verification.submit();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          a.verification.review(DevelopmentVerification.State.APPROVED);
          a.flow = "qr";
          a.render();
          ImageView image = findQr(a.content);
          assertNotNull(image);
          Bitmap bitmap =
              ((android.graphics.drawable.BitmapDrawable) image.getDrawable()).getBitmap();
          int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
          bitmap.getPixels(
              pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
          try {
            com.google.zxing.Result decoded =
                new com.google.zxing.qrcode.QRCodeReader()
                    .decode(
                        new com.google.zxing.BinaryBitmap(
                            new com.google.zxing.common.HybridBinarizer(
                                new com.google.zxing.RGBLuminanceSource(
                                    bitmap.getWidth(), bitmap.getHeight(), pixels))));
            assertEquals(a.qrPayload, decoded.getText());
            assertTrue(
                decoded
                    .getText()
                    .startsWith("vastudents-test://student-id?status=synthetic-only&expires="));
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          String prior = a.qrPayload;
          a.qrExpiresAt = System.currentTimeMillis() - 1;
          a.render();
          assertNull(findQr(a.content));
          assertTrue(AppFlowTest.contains(a.content, "测试二维码已过期"));
          assertEquals(prior, a.qrPayload);
          a.verification.reset();
        });
  }

  static ImageView findQr(View v) {
    if (v instanceof ImageView) return (ImageView) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        ImageView found = findQr(((ViewGroup) v).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }

  void screen(String name) throws Exception {
    inst.waitForIdleSync();
    Thread.sleep(400);
    Bitmap bitmap = inst.getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    File dir = new File(a.getExternalFilesDir(null), "screenshots");
    assertTrue(dir.exists() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }
}
