#import "RNDNSSD.h"


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