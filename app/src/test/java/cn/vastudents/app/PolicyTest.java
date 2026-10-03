package cn.vastudents.app;

import static org.junit.Assert.*;

import java.time.LocalDate;
import org.junit.Test;

public class PolicyTest {
  Offer sample() {
    Offer o = new Offer();
    o.id = "fixture";
    o.title = "非常长的中文学生优惠标题用于验证搜索不会被截断";
    o.merchant = "Test Brand";
    o.summary = "测试";
    o.category = "数码";
    o.verifiedAt = "2026-10-03";
    o.status = "published";
    o.sourceStatus = "public_page_checked";
    o.channel = "online";
    return o;
  }

  @Test
  public void chineseAndCaseInsensitiveSearch() {
    Offer o = sample();
    assertTrue(o.matches("非常长的中文", "全部"));
    assertTrue(o.matches("test BRAND", "数码"));
    assertFalse(o.matches("学生", "影音"));
  }

  @Test
  public void expiryIsInclusiveAndOverridesVerification() {
    Offer o = sample();
    o.expiresAt = "2026-10-03";
    assertTrue(o.actionable(LocalDate.parse("2026-10-03")));
    assertEquals("已过期", o.freshness(LocalDate.parse("2026-10-04")));
  }

  @Test
  public void staleSourcesCannotAppearCurrentlyVerified() {
    Offer o = sample();
    assertEquals("待重新核实", o.freshness(LocalDate.parse("2026-11-03")));
    assertFalse(o.actionable(LocalDate.parse("2026-11-03")));
  }

  @Test
  public void withdrawnAndTestDataNotRedeemable() {
    Offer o = sample();
    o.status = "withdrawn";
    assertFalse(o.actionable(LocalDate.parse("2026-10-03")));
    o.status = "published";
    o.isTestData = true;
    assertFalse(o.actionable(LocalDate.parse("2026-10-03")));
  }

  @Test
  public void storesRequireExplicitApplicability() {
    Offer o = sample();
    o.storeIds.add("one-verified-store");
    assertFalse(o.appliesTo("one-verified-store"));
    o.channel = "offline";
    assertTrue(o.appliesTo("one-verified-store"));
    assertFalse(o.appliesTo("another-branch"));
  }

  @Test
  public void walletNeverGrantsUnbackedEntitlements() {
    WalletState w = new WalletState();
    assertFalse(w.canRedeem());
    assertEquals(WalletState.Qualification.NOT_VERIFIED, w.qualification);
    assertEquals(0, w.realRedemptionCount);
    assertFalse(w.merchantIntegrationEnabled);
  }

  @Test
  public void repeatedDemoRequestsAreIdempotentAndExpire() {
    WalletState w = new WalletState();
    String code = w.demoCode(1000);
    assertTrue(code.startsWith("TEST-"));
    assertEquals(code, w.demoCode(1001));
    assertEquals(60, w.remainingSeconds(1001));
    assertEquals(0, w.remainingSeconds(61000));
    assertNotEquals(code, w.demoCode(61000));
    assertEquals(0, w.realRedemptionCount);
  }
}
