
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
  	project(':react-native-dnssd').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-dnssd/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-dnssd')
  	```


## Usage
```javascript
import RNDnssd from 'react-native-dnssd';

// TODO: What to do with the module?
RNDnssd;
```
  