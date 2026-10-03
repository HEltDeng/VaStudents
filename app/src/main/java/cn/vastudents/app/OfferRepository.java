package cn.vastudents.app;

import android.content.Context;
import java.io.*;
import java.net.URI;
import java.util.*;
import org.json.*;

public final class OfferRepository {
  public final List<Offer> offers = new ArrayList<>();
  public final Map<String, Store> stores = new LinkedHashMap<>();

  public static final class Store {
    public String id,
        name,
        address,
        city,
        sourceUrl,
        verifiedAt,
        coordinateSourceUrl,
        coordinateNote,
        navigationUrl;
    public double latitude, longitude;
  }

  public void load(Context context) throws Exception {
    String raw;
    try (InputStream in = context.getAssets().open("offers.json")) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[4096];
      int n;
      while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
      raw = buffer.toString("UTF-8");
    }
    parse(raw);
  }

  public void parse(String raw) throws Exception {
    JSONObject root = new JSONObject(raw);
    if (root.getInt("schemaVersion") != 1) throw new IOException("Unsupported schema");
    List<Offer> next = new ArrayList<>();
    Set<String> ids = new HashSet<>(), urls = new HashSet<>();
    JSONArray data = root.getJSONArray("offers");
    for (int i = 0; i < data.length(); i++) {
      JSONObject j = data.getJSONObject(i);
      Offer o = new Offer();
      o.id = j.getString("id");
      o.title = j.getString("title");
      o.merchant = j.getString("merchant");
      o.category = j.getString("category");
      o.summary = j.getString("summary");
      o.benefit = j.getString("benefit");
      o.eligibility = j.getString("eligibility");
      o.validity = j.getString("validity");
      o.sourceUrl = j.getString("sourceUrl");
      o.verifiedAt = j.getString("verifiedAt");
      o.channel = j.getString("channel");
      o.status = j.getString("status");
      o.sourceStatus = j.getString("sourceStatus");
      o.expiresAt = j.isNull("expiresAt") ? null : j.getString("expiresAt");
      o.limits = j.getString("limits");
      o.isTestData = j.getBoolean("isTestData");
      if (!"https".equals(new URI(o.sourceUrl).getScheme()))
        throw new IOException("HTTPS source required");
      java.time.LocalDate.parse(o.verifiedAt);
      if (o.expiresAt != null) java.time.LocalDate.parse(o.expiresAt);
      if (!Arrays.asList("online", "offline").contains(o.channel))
        throw new IOException("Invalid channel");
      if (!Arrays.asList("published", "withdrawn").contains(o.status))
        throw new IOException("Invalid status");
      JSONArray s = j.getJSONArray("storeIds");
      for (int k = 0; k < s.length(); k++) o.storeIds.add(s.getString(k));
      if (ids.add(o.id) && urls.add(o.sourceUrl)) next.add(o);
    }
    Map<String, Store> nextStores = new LinkedHashMap<>();
    JSONArray rows = root.getJSONArray("stores");
    for (int i = 0; i < rows.length(); i++) {
      JSONObject j = rows.getJSONObject(i);
      Store s = new Store();
      s.id = j.getString("id");
      s.name = j.getString("name");
      s.address = j.getString("address");
      s.city = j.getString("city");
      s.sourceUrl = j.getString("sourceUrl");
      s.verifiedAt = j.getString("verifiedAt");
      s.latitude = j.optDouble("latitude", Double.NaN);
      s.longitude = j.optDouble("longitude", Double.NaN);
      s.coordinateSourceUrl = j.optString("coordinateSourceUrl", "");
      s.coordinateNote = j.optString("coordinateNote", "入口点位尚未核实");
      s.navigationUrl = j.optString("navigationUrl", s.sourceUrl);
      if (!"https".equals(new URI(s.navigationUrl).getScheme()))
        throw new IOException("HTTPS navigation required");
      if (Math.abs(s.latitude) > 90 || Math.abs(s.longitude) > 180)
        throw new IOException("Invalid coordinate");
      java.time.LocalDate.parse(s.verifiedAt);
      if (!"https".equals(new URI(s.sourceUrl).getScheme()))
        throw new IOException("Store source required");
      nextStores.put(s.id, s);
    }
    for (Offer o : next)
      for (String id : o.storeIds)
        if (!nextStores.containsKey(id)) throw new IOException("Unknown store");
    offers.clear();
    offers.addAll(next);
    stores.clear();
    stores.putAll(nextStores);
  }
}
