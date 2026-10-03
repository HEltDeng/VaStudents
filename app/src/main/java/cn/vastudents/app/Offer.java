package cn.vastudents.app;

import java.time.LocalDate;
import java.util.*;

public final class Offer {
  public String id,
      merchant,
      title,
      category,
      summary,
      benefit,
      eligibility,
      validity,
      sourceUrl,
      verifiedAt,
      channel,
      status,
      expiresAt,
      limits,
      sourceStatus;
  public boolean isTestData;
  public final List<String> storeIds = new ArrayList<>();

  public boolean matches(String query, String categoryFilter) {
    String q = query.trim().toLowerCase(Locale.ROOT);
    return (categoryFilter.equals("全部") || category.equals(categoryFilter))
        && (title + merchant + summary + category).toLowerCase(Locale.ROOT).contains(q);
  }

  public String freshness(LocalDate today) {
    if ("withdrawn".equals(status)) return "已下架";
    if (expiresAt != null && !expiresAt.isEmpty() && LocalDate.parse(expiresAt).isBefore(today))
      return "已过期";
    if (!"public_page_checked".equals(sourceStatus)) return "待核实";
    if (LocalDate.parse(verifiedAt).plusDays(30).isBefore(today)) return "待重新核实";
    return "公开页面已核实";
  }

  public boolean actionable(LocalDate today) {
    return "公开页面已核实".equals(freshness(today)) && !isTestData;
  }

  public boolean appliesTo(String storeId) {
    return "offline".equals(channel) && storeIds.contains(storeId);
  }
}
