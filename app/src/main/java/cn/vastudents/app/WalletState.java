package cn.vastudents.app;

/** Qualification, entitlement rules and redemption facts deliberately remain separate. */
public final class WalletState {
  public enum Qualification {
    NOT_VERIFIED,
    PENDING,
    VERIFIED,
    EXPIRED
  }

  public Qualification qualification = Qualification.NOT_VERIFIED;
  public boolean merchantIntegrationEnabled = false;
  public int realRedemptionCount = 0;
  private String demoCode;
  private long demoExpiresAt;

  public boolean canRedeem() {
    return false;
  } // No merchant contract or server in v0.1.

  public String demoCode(long now) {
    if (demoCode == null || now >= demoExpiresAt) {
      demoCode =
          "TEST-"
              + java.util
                  .UUID
                  .randomUUID()
                  .toString()
                  .substring(0, 8)
                  .toUpperCase(java.util.Locale.ROOT);
      demoExpiresAt = now + 60000;
    }
    return demoCode;
  }

  public long remainingSeconds(long now) {
    return Math.max(0, (demoExpiresAt - now + 999) / 1000);
  }
}
