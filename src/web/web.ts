import { WebPlugin } from '@capacitor/core';
import { TCPClientPlugin } from './definitions';

export class TCPClientWeb extends WebPlugin implements TCPClientPlugin {
  constructor() {
    super({
      name: 'TCPClient',
      platforms: ['web']
    });
  }

  async connect(options: { host: string; port: number }): Promise<{ success: boolean }> {
    console.log('Web implementation not available');
    return { success: false };
  }

  async send(options: { data: string }): Promise<{ success: boolean }> {
    console.log('Web implementation not available');
    return { success: false };
  }

  async disconnect(): Promise<{ success: boolean }> {
    console.log('Web implementation not available');
    return { success: false };
  }
} 