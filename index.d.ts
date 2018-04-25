export declare namespace DNSSD {
    type ServiceFound = (service: Service) => void;
    type ServiceLost = (service: Service) => void;
    type ServiceResolved = (service: ResolvedService) => void;
    interface Subscription {
        remove(): void;
    }
    function addEventListener(event: "serviceFound", listener: ServiceFound): Subscription;
    function addEventListener(event: "serviceLost", listener: ServiceLost): Subscription;
    function addEventListener(event: "serviceResolved", listener: ServiceResolved): Subscription;
    function startSearch(type: string, protocol?: string, domain?: string): void;
    function stopSearch(): void;
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
