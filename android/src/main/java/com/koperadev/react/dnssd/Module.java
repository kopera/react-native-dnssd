
package com.koperadev.react.dnssd;

import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Nullable;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDEmbedded;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;


public class Module extends ReactContextBaseJavaModule {
  private final ReactContext reactContext;
  private final DNSSD dnssd;
  private final ArrayList<DNSSDService> searches;
  private final ArrayList<DNSSDService> resolutions;

  public static final String TAG = "RNDNSSD";

  public Module(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    dnssd = new DNSSDEmbedded(60000);
    searches = new ArrayList<>();
    resolutions = new ArrayList<>();
  }

  @Override
  public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    stopSearch();
  }

  @Override
  public String getName() {
    return "RNDNSSD";
  }

  @ReactMethod
  public void startSearch(String type, String protocol, String domain) {
    String serviceType = String.format("_%s._%s", type, protocol);

    Log.d(TAG, "Search starting for " + serviceType + " in domain: " + domain);
    try {
      DNSSDService search = dnssd.browse(0, 0, serviceType, domain, new BrowseListener());
      Log.d(TAG, "Search started for " + serviceType + " in domain: " + domain);
      searches.add(search);
    } catch (DNSSDException e) {
      Log.e(TAG, "Search start error: " + e, e);
    }
  }

  @ReactMethod
  public void stopSearch() {
    Log.d(TAG, "Stop all searches");
    for (DNSSDService resolution: resolutions) {
      resolution.stop();
    }
    for (DNSSDService search: searches) {
      search.stop();
    }
    searches.clear();
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

  private class BrowseListener implements com.github.druk.dnssd.BrowseListener {
    @Override
    public void serviceFound(DNSSDService search, int flags, int ifIndex, final String serviceName, String regType, String domain) {
      try {
        String serviceId = dnssd.constructFullName(serviceName, regType, domain);
        Log.d(TAG, "Service Found: " + serviceId);

        WritableMap service = new WritableNativeMap();
        service.putString("name", serviceName);
        service.putString("type", regType);
        service.putString("domain", domain);

        sendEvent("serviceFound", service);

        DNSSDService resolution = dnssd.resolve(0, 0, serviceName, regType, domain, new ResolveListener(serviceName, regType, domain));
        resolutions.add(resolution);
      } catch (DNSSDException e) {
        Log.e(TAG, "Resolve start error: " + e, e);
      }
    }

    @Override
    public void serviceLost(DNSSDService search, int flags, int ifIndex, String serviceName, String regType, String domain) {
      try {
        String serviceId = dnssd.constructFullName(serviceName, regType, domain);
        Log.d(TAG, "Service Lost: " + serviceId);

        WritableMap service = new WritableNativeMap();
        service.putString("id", serviceId);
        service.putString("name", serviceName);
        service.putString("type", regType);
        service.putString("domain", domain);

        sendEvent("serviceLost", service);
      } catch (DNSSDException e) {
        Log.e(TAG, "Service lost error: " + e, e);
      }
    }

    @Override
    public void operationFailed(DNSSDService search, int errorCode) {
      Log.e(TAG, "Browse error: " + errorCode);
    }
  }

  private class ResolveListener implements com.github.druk.dnssd.ResolveListener {
    private final String serviceName;
    private final String regType;
    private final String domain;

    public ResolveListener(String serviceName, String regType, String domain) {
      this.serviceName = serviceName;
      this.regType = regType;
      this.domain = domain;
    }

    @Override
    public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, Map<String, String> txtRecord) {
      Log.d(TAG, "Service Resolved: " + fullName);

      WritableMap service = new WritableNativeMap();
      service.putString("id", fullName);
      service.putString("name", serviceName);
      service.putString("type", regType);
      service.putString("domain", domain);
      service.putString("hostName", hostName);
      service.putInt("port", port);

      WritableMap txt = new WritableNativeMap();
      for (Map.Entry<String, String> entry : txtRecord.entrySet()) {
        txt.putString(entry.getKey(), entry.getValue());
      }
      service.putMap("txt", txt);

      sendEvent("serviceResolved", service);
      resolver.stop();
    }

    @Override
    public void operationFailed(DNSSDService resolver, int errorCode) {
      Log.e(TAG, "Resolve error: " + errorCode);
      resolver.stop();
    }
  }
}