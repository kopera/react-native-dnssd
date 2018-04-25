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
