
package com.koperadev.react.dnssd;

import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Nullable;

import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.github.druk.rxdnssd.RxDnssdEmbedded;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class Module extends ReactContextBaseJavaModule {
  private final ReactContext reactContext;
  private final RxDnssd dnssd;
  private final ArrayList<Subscription> searches;

  public static final String TAG = "RNDNSSD";

  public Module(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
        || (Build.VERSION.RELEASE.contains("4.4.2") && Build.MANUFACTURER.toLowerCase().contains("samsung"))) {
      dnssd = new RxDnssdEmbedded();
    } else {
      dnssd = new RxDnssdBindable(reactContext);
    }
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
    String serviceType = String.format("_%s._%s", type, protocol);

    Log.d(TAG, "Search starting for " + serviceType + " in domain: " + domain);
    Subscription subscription = dnssd.browse(serviceType, domain)
      .compose(dnssd.resolve())
      .compose(dnssd.queryRecords())
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Action1<BonjourService>() {
        @Override
          public void call(BonjourService bonjourService) {
            WritableMap service = new WritableNativeMap();
            service.putString("name", bonjourService.getServiceName());
            service.putString("type", bonjourService.getRegType());
            service.putString("domain", bonjourService.getDomain());
            service.putString("hostName", bonjourService.getHostname());
            service.putInt("port", bonjourService.getPort());
      
            WritableMap txt = new WritableNativeMap();
            for (Map.Entry<String, String> entry : bonjourService.getTxtRecords().entrySet()) {
              txt.putString(entry.getKey(), entry.getValue());
            }
            service.putMap("txt", txt);

            if (bonjourService.isLost()) {
              Log.d(TAG, "Service Lost: " + bonjourService);
              sendEvent("serviceLost", service);
            } else {
              Log.d(TAG, "Service Found: " + bonjourService);
              sendEvent("serviceFound", service);
            }
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "error", throwable);
          }
      });

      searches.add(subscription);
  }

  @ReactMethod
  public void stopSearch() {
    Log.d(TAG, "Stop all searches");
    for (Subscription search: searches) {
      search.unsubscribe();
    }
    searches.clear();
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }
}