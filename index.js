import { NativeEventEmitter, NativeModules } from "react-native";
export var DNSSD;
(function (DNSSD) {
    const Implementation = NativeModules.RNDNSSD;
    const EventEmitter = new NativeEventEmitter(Implementation);
    function addEventListener(event, listener) {
        return EventEmitter.addListener(event, listener);
    }
    DNSSD.addEventListener = addEventListener;
    function startSearch(type, protocol = "tcp", domain = "") {
        return Implementation.startSearch(type, protocol, domain);
    }
    DNSSD.startSearch = startSearch;
    function stopSearch() {
        return Implementation.stopSearch();
    }
    DNSSD.stopSearch = stopSearch;
})(DNSSD || (DNSSD = {}));
