export declare namespace DNSSD {
    type ServiceFound = (service: Service) => void;
    type ServiceLost = (service: Service) => void;
    interface Subscription {
        remove(): void;
    }
    function addEventListener(event: "serviceFound", listener: ServiceFound): Subscription;
    function addEventListener(event: "serviceLost", listener: ServiceLost): Subscription;
    function startSearch(type: string, protocol?: string): void;
    function stopSearch(): void;
}
/** Types */
export interface Service {
    readonly name: string;
    readonly type: string;
    readonly domain: string;
    readonly hostName: string;
    readonly port: number;
    readonly txt: Record<string, string>;
}
