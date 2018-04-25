
package com.koperadev.react.dnssd;

import javax.annotation.Nullable;

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

import java.util.ArrayList;
import java.util.Map;

public class Module extends ReactContextBaseJavaModule {
  private final ReactContext reactContext;
  private final DNSSD dnssd;
  private final ArrayList<DNSSDService> searches;

  public Module(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    dnssd = new DNSSDEmbedded();
    searches = new ArrayList<>();
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
    String serviceType = String.format("_%s._%s.", type, protocol);

    try {
      DNSSDService search = dnssd.browse(0, 0, serviceType, domain, new BrowseListener());
      searches.add(search);
    } catch (DNSSDException e) {
      // Log.e("TAG", "error: " + errorCode);
    }
  }

  @ReactMethod
  public void stopSearch() {
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
      WritableMap service = new WritableNativeMap();
      service.putString("name", serviceName);
      service.putString("type", regType);
      service.putString("domain", domain);

      sendEvent("serviceFound", service);

      try {
        dnssd.resolve(0, 0, serviceName, regType, domain, new ResolveListener());
      } catch (DNSSDException e) {
        // Log.e("TAG", "error: " + errorCode);
      }
    }

    @Override
    public void serviceLost(DNSSDService search, int flags, int ifIndex, String serviceName, String regType, String domain) {
      WritableMap service = new WritableNativeMap();
      service.putString("name", serviceName);
      service.putString("type", regType);
      service.putString("domain", domain);

      sendEvent("serviceLost", service);
    }

    @Override
    public void operationFailed(DNSSDService search, int errorCode) {
      // Log.e("TAG", "error: " + errorCode);
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
      WritableMap service = new WritableNativeMap();
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
    }

    @Override
    public void operationFailed(DNSSDService resolver, int errorCode) {

    }
  }
}