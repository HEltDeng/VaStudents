package cn.vastudents.app;

import android.content.*;
import java.io.*;

/** Synthetic UI workflow only. Never an authority for real identity or merchant entitlements. */
final class DevelopmentVerification {
  enum State {
    NONE,
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    REVOKED
  }

  private final Context context;
  private final SharedPreferences prefs;
  private static final long RETENTION_MS = 7L * 24 * 60 * 60 * 1000;

  DevelopmentVerification(Context c) {
    context = c;
    prefs = c.getSharedPreferences("synthetic_verification", Context.MODE_PRIVATE);
    if (BuildConfig.DEVELOPMENT_FEATURES) purgeExpiredMaterial(System.currentTimeMillis());
  }

  private void requireDev() {
    if (!BuildConfig.DEVELOPMENT_FEATURES)
      throw new SecurityException("Development workflow disabled in this build");
  }

  boolean loggedIn() {
    return BuildConfig.DEVELOPMENT_FEATURES && prefs.getBoolean("testSession", false);
  }

  void login() {
    requireDev();
    prefs.edit().putBoolean("testSession", true).apply();
  }

  State state() {
    if (!loggedIn()) return State.NONE;
    State s;
    try {
      s = State.valueOf(prefs.getString("state", "NONE"));
    } catch (Exception e) {
      return State.NONE;
    }
    if (s == State.APPROVED && System.currentTimeMillis() >= prefs.getLong("validUntil", 0))
      return State.EXPIRED;
    return s;
  }

  File material() {
    return new File(context.getNoBackupFilesDir(), "synthetic-student-material.txt");
  }

  void submit() throws IOException {
    requireDev();
    if (!loggedIn()) throw new SecurityException("Test session required");
    State s = state();
    if (s == State.PENDING) return;
    if (s == State.APPROVED) throw new IllegalStateException("Already approved test case");
    try (FileOutputStream out = new FileOutputStream(material())) {
      out.write(
          "SYNTHETIC TEST ONLY\nNot a CHSI document. No real person or student data.\nFictional institution: Test University\n"
              .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    prefs
        .edit()
        .putString("state", State.PENDING.name())
        .putLong("deleteAfter", System.currentTimeMillis() + RETENTION_MS)
        .apply();
  }

  void review(State target) {
    requireDev();
    State s = state();
    boolean allowed =
        (s == State.PENDING && (target == State.APPROVED || target == State.REJECTED))
            || (s == State.APPROVED && (target == State.EXPIRED || target == State.REVOKED));
    if (!allowed) throw new IllegalStateException("Invalid test transition");
    if (target == State.APPROVED && !material().isFile())
      throw new IllegalStateException("Test material missing");
    prefs
        .edit()
        .putString("state", target.name())
        .putLong(
            "validUntil",
            target == State.APPROVED ? System.currentTimeMillis() + 24 * 60 * 60 * 1000 : 0)
        .apply();
  }

  void purgeExpiredMaterial(long now) {
    requireDev();
    if (material().exists() && now >= prefs.getLong("deleteAfter", 0)) {
      if (!material().delete())
        throw new IllegalStateException("Unable to delete expired test material");
      if (State.PENDING.name().equals(prefs.getString("state", "NONE")))
        prefs.edit().putString("state", State.EXPIRED.name()).apply();
    }
  }

  void reset() {
    requireDev();
    if (material().exists() && !material().delete())
      throw new IllegalStateException("Unable to delete test material");
    prefs.edit().clear().apply();
  }

  String label() {
    switch (state()) {
      case PENDING:
        return "演示 · 待审核";
      case APPROVED:
        return "演示 · 已通过（非真实认证）";
      case REJECTED:
        return "演示 · 未通过";
      case EXPIRED:
        return "演示 · 已到期";
      case REVOKED:
        return "演示 · 已撤销";
      default:
        return "尚未验证学生身份";
    }
  }

  boolean isRealVerified() {
    return false;
  }
}
