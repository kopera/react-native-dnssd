
import { NativeEventEmitter, NativeModules } from "react-native";

export namespace DNSSD {
  export type ServiceFound = (service: Service) => void;
  export type ServiceLost = (service: Service) => void;
  export type ServiceResolved = (service: ResolvedService) => void;

  export interface Subscription {
    remove(): void;
  }

  const Implementation = NativeModules.RNDNSSD;
  const EventEmitter = new NativeEventEmitter(Implementation);

  export function addEventListener(event: "serviceFound", listener: ServiceFound): Subscription;
  export function addEventListener(event: "serviceLost", listener: ServiceLost): Subscription;
  export function addEventListener(event: "serviceResolved", listener: ServiceResolved): Subscription;
  export function addEventListener(event: string, listener: any): Subscription {
    return EventEmitter.addListener(event, listener);
  }

  export function startSearch(type: string, protocol: string = "tcp", domain: string = ""): void {
    return Implementation.startSearch(type, protocol, domain);
  }

  export function stopSearch(): void {
    return Implementation.stopSearch();
  }
}

/** Types */

export interface Service {
  readonly name: string;
  readonly type: string;
  readonly domain: string;
}

export interface ResolvedService extends Service {
  readonly hostName: string;
  readonly port: number;
  readonly txt: Record<string, string>;
}