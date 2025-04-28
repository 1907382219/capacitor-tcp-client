import { registerPlugin } from '@capacitor/core';

const TCPClient = registerPlugin('TCPClient', {
  web: () => import('./web').then(m => new m.TCPClientWeb()),
});

export { TCPClient }; 