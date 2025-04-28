import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.example.tcpclient',
  appName: 'TCP Client',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
};

export default config; 