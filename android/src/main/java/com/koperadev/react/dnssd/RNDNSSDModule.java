
package com.koperadev.react.dnssd;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Nullable;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import de.mannodermaus.rxbonjour.BonjourEvent;
import de.mannodermaus.rxbonjour.BonjourService;
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver;
import de.mannodermaus.rxbonjour.platforms.android.AndroidPlatform;
import de.mannodermaus.rxbonjour.RxBonjour;


public class RNDNSSDModule extends ReactContextBaseJavaModule {
  private final RxBonjour dnssd;
  private final ArrayList<Disposable> searches;

  public static final String TAG = "RNDNSSD";

  public RNDNSSDModule(ReactApplicationContext reactContext) {
    super(reactContext);

    dnssd = new RxBonjour.Builder()
      .platform(AndroidPlatform.create(reactContext))
      .driver(JmDNSDriver.create())
      .create();
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
  public void startSearch(String type, String protocol) {
    String serviceType = String.format("_%s._%s", type, protocol);

    Log.d(TAG, "Search starting for " + serviceType + " in domain: local.");
    Disposable search = dnssd.newDiscovery(serviceType)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        new Consumer<BonjourEvent>() {
          @Override
          public void accept(BonjourEvent event) throws Exception {
            BonjourService bonjourService = event.getService();
            InetAddress host = bonjourService.getHost();

            WritableMap service = new WritableNativeMap();
            service.putString("name", bonjourService.getName());
            service.putString("type", bonjourService.getType().replaceAll("\\.local\\.$", "."));
            service.putString("domain", "local.");
            if (host != null) {
              service.putString("hostName", host.getHostAddress());
            } else {
              service.putNull("hostName");
            }
            service.putInt("port", bonjourService.getPort());

            WritableMap txt = new WritableNativeMap();
            for (Map.Entry<String, String> entry : bonjourService.getTxtRecords().entrySet()) {
              txt.putString(entry.getKey(), entry.getValue());
            }
            service.putMap("txt", txt);

            WritableArray addresses = Arguments.createArray();
            Inet4Address ipv4Address = bonjourService.getV4Host();
            if (ipv4Address != null) {
              addresses.pushString(ipv4Address.getHostAddress());
            }
            Inet6Address ipv6Address = bonjourService.getV6Host();
            if (ipv6Address != null) {
              addresses.pushString(ipv6Address.getHostAddress());
            }
            service.putArray("addresses", addresses);


            if (event instanceof BonjourEvent.Added) {
              Log.d(TAG, "Service Found: " + bonjourService);
              sendEvent("serviceFound", service);
            } else if (event instanceof BonjourEvent.Removed) {
              Log.d(TAG, "Service Lost: " + bonjourService);
              sendEvent("serviceLost", service);
            }
          }
        },
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable e) throws Exception {
            Log.e(TAG, "error", e);
          }
        }
      );

      searches.add(search);
  }

  @ReactMethod
  public void stopSearch() {
    Log.d(TAG, "Stop all searches");
    for (Disposable search: searches) {
      search.dispose();
    }
    searches.clear();
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    getReactApplicationContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }
}