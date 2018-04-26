
# react-native-dnssd

## Getting started

`$ npm install react-native-dnssd --save`

### Mostly automatic installation

`$ react-native link react-native-dnssd`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-dnssd` and add `RNDnssd.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNDnssd.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.koperadev.RNDnssdPackage;` to the imports at the top of the file
  - Add `new RNDnssdPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
    ```
    include ':react-native-dnssd'
    project(':react-native-dnssd').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-dnssd/android')
    ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
    ```
      compile project(':react-native-dnssd')
    ```

## Usage
```javascript

import { DNSSD } from 'react-native-dnssd';

const foundSub = DNSSD.addEventListener("serviceFound", (service) => {
	console.log("Service Found", service);
});
const lostSub = DNSSD.addEventListener("serviceLost", (service) => {
	console.log("Service Lost", service);
});
DNSSD.startSearch("airplay", "tcp");

setTimeout(() => {
	DNSSD.stopSearch();
	foundSub.remove();
	lostSub.remove();
}, 30000);

```

## Example
```javascript
import { DNSSD } from 'react-native-dnssd';

import React, { Component } from 'react';
import {
  FlatList,
  Platform,
  SafeAreaView,
  StyleSheet,
  Text,
  View
} from 'react-native';
import { DNSSD } from 'react-native-dnssd';

export default class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      services: [],
    };
  }

  componentDidMount() {
    this.serviceFoundSub = DNSSD.addEventListener("serviceFound", (service) => {
      console.log("Service Found", service);
      const existing = this.state.services.find((s) => s.name === service.name);
      this.setState({
        services: existing
          ? this.state.services.map((s) => s.name === service.name ? service : s)
          : [...this.state.services, service],
      });
    });
    this.serviceLostSub = DNSSD.addEventListener("serviceLost", (service) => {
      console.log("Service Lost", service);
      this.setState({
        services: this.state.services.filter((s) => s.name !== service.name),
      });
    });
    DNSSD.startSearch("airplay", "tcp");
  }

  componentWillUnmount() {
    this.serviceFoundSub.remove();
    this.serviceLostSub.remove();
    this.serviceResolvedSub.remove();
    DNSSD.stopSearch();
  }

  render() {
    const { services } = this.state;

    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.content}>
          <Text style={styles.title}>Airplay Devices</Text>
          <FlatList
            data={services}
            keyExtractor={(service) => `${service.name}.${service.type}${service.domain}`}
            renderItem={({ item: service }) => <Service service={service} />}
            style={styles.services} />
        </View>
      </SafeAreaView>
    );
  }
}

const Service = ({ service }) =>
  <View style={styles.service}>
    <Text style={styles.serviceName}>{service.name}</Text>
    {service.hostName !== undefined
      ? <Text style={styles.serviceHost}>{service.hostName}:{service.port}</Text>
      : null}
    {Object.keys(service.txt || {}).map((key) =>
      <Text key={key} style={styles.serviceTxt}>{key}={service.txt[key]}</Text>)}
  </View>;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0faff',
  },
  content: {
    flex: 1,
    padding: 20,
  },
  title: {
    marginBottom: 20,

    fontSize: 28,
  },
  services: {
    flex: 1,
  },
  service: {
    paddingBottom: 10,
  },
  serviceName: {
    fontSize: 20,
  },
  serviceType: {
    color: '#aaaaaa',
  },
  serviceHost: {
    color: '#aaaaaa',
  },
  serviceTxt: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace'
  },
});

```
  