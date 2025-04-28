import { registerPlugin } from '@capacitor/core';
import type { TcpClientPlugin } from './definitions';

const TcpClient = registerPlugin<TcpClientPlugin>('TcpClient', {
  web: () => import('./web').then(m => new m.TcpClientWeb()),
});

export * from './definitions';
export { TcpClient };