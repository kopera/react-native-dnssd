Pod::Spec.new do |s|
  s.name         = "RNDnssd"
  s.version      = "1.0.0"
  s.summary      = "React Native DNS SD"
  s.description  = <<-DESC
    React Native DNS SD
  DESC
  s.homepage     = ""
  s.license      = "MIT"
  s.author       = { "Ali Sabil" => "ali.sabil@gmail.com" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/koperadev/react-native-dnssd.git", :tag => "master" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
end