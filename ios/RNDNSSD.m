#import "RNDNSSD.h"
#include <arpa/inet.h>

@interface RNDNSSD () <NSNetServiceBrowserDelegate, NSNetServiceDelegate>

- (NSDictionary<NSString *, id> *) serviceToJson: (NSNetService *) service;
- (NSDictionary<NSString *, id> *) serviceTXT: (NSNetService *) service;

@end

@implementation RNDNSSD
{
  NSNetServiceBrowser *_browser;
  NSMutableDictionary *_services;
  BOOL _hasListeners;
}

RCT_EXPORT_MODULE()

#pragma mark - Lifecycle

- (void) dealloc
{
  [_browser stop];
  _browser.delegate = nil;
}

- (NSArray<NSString *> *) supportedEvents
{
  return @[
           @"serviceFound",
           @"serviceLost",
           ];
}

- (dispatch_queue_t) methodQueue
{
  return dispatch_get_main_queue();
}

- (void) startObserving
{
  _hasListeners = YES;
}

- (void) stopObserving
{
  _hasListeners = NO;
}

#pragma mark - Public API

RCT_EXPORT_METHOD(startSearch:(NSString *)type protocol:(NSString *)protocol)
{
  if (!_browser) {
    _browser = [[NSNetServiceBrowser alloc] init];
    [_browser setDelegate:self];
    
    _services = [[NSMutableDictionary alloc] init];
  }
  
  [_browser searchForServicesOfType:[NSString stringWithFormat:@"_%@._%@.", type, protocol] inDomain:@"local."];
}

RCT_EXPORT_METHOD(stopSearch)
{
  if (_browser) {
    [_browser stop];
    for (id name in _services) {
      [[_services objectForKey:name] stop];
    }
    [_services removeAllObjects];
  }
}

#pragma mark - Private methods

- (NSDictionary <NSString *, id> *) serviceToJson: (NSNetService *) service
{
  return @{
           @"name": service.name,
           @"type": service.type,
           @"domain": service.domain,
           @"hostName": service.hostName,
           @"port": @(service.port),
           @"txt": [self serviceTXT:service],
           @"addresses": [self addressesFromService:service],
           };
}

- (NSDictionary <NSString *, id> *) serviceTXT: (NSNetService *) service
{
  NSDictionary<NSString *, NSData *> *txtDict = [NSNetService dictionaryFromTXTRecordData:service.TXTRecordData];
  NSMutableDictionary *txt = [[NSMutableDictionary alloc] init];
  for (NSString *key in txtDict) {
    txt[key] = [[NSString alloc]
                initWithData:txtDict[key]
                encoding:NSUTF8StringEncoding];
  }
  
  return [NSDictionary dictionaryWithDictionary:txt];
}

- (NSArray<NSString *> *) addressesFromService:(NSNetService *)service
{
  NSMutableArray<NSString *> *addresses = [[NSMutableArray alloc] init];
  
  // source: http://stackoverflow.com/a/4976808/2715
  char addressBuffer[INET6_ADDRSTRLEN];
  
  for (NSData *data in service.addresses) {
    memset(addressBuffer, 0, INET6_ADDRSTRLEN);
    
    typedef union {
      struct sockaddr sa;
      struct sockaddr_in ipv4;
      struct sockaddr_in6 ipv6;
    } ip_socket_address;
    
    ip_socket_address *socketAddress = (ip_socket_address *)[data bytes];
    
    if (socketAddress && (socketAddress->sa.sa_family == AF_INET || socketAddress->sa.sa_family == AF_INET6)) {
      const char *addressStr = inet_ntop(
                                         socketAddress->sa.sa_family,
                                         (socketAddress->sa.sa_family == AF_INET ? (void *)&(socketAddress->ipv4.sin_addr) : (void *)&(socketAddress->ipv6.sin6_addr)),
                                         addressBuffer,
                                         sizeof(addressBuffer)
                                         );
      
      if (addressStr) {
        NSString *address = [NSString stringWithUTF8String:addressStr];
        [addresses addObject:address];
      }
    }
  }
  
  return [NSArray arrayWithArray:addresses];
}

#pragma mark - NSNetServiceBrowserDelegate

- (void) netServiceBrowser:(NSNetServiceBrowser *)browser
            didFindService:(NSNetService *)service
                moreComing:(BOOL)moreComing
{
  if (service == nil) {
    return;
  }
  
  _services[service.name] = service;
  service.delegate = self;
  [service resolveWithTimeout:0.0];
}

- (void) netServiceBrowser:(NSNetServiceBrowser*)netServiceBrowser
          didRemoveService:(NSNetService*)service
                moreComing:(BOOL)moreComing
{
  if (service == nil) {
    return;
  }

  NSString *name = service.name;
  if (_services[name]) {
    NSNetService *service = _services[name];
    [_services removeObjectForKey:service.name];
    [service stop];
    
    if (_hasListeners && service.hostName != nil) {
      [self sendEventWithName: @"serviceLost"
                         body: [self serviceToJson:service]];
    }
  }
}

#pragma mark - NSNetServiceDelegate

- (void) netServiceDidResolveAddress:(NSNetService *)service
{
  if (_services[service.name]) {
    _services[service.name] = service;

    if (_hasListeners) {
      [self sendEventWithName: @"serviceFound"
                         body: [self serviceToJson:service]];
    }
  }
}

@end