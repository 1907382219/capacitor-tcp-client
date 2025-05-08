// src/web.ts
import { WebPlugin } from '@capacitor/core';

import type { TcpClientSendOnceOpt, TcpClientConnectOpt, TcpClientConnectResultOpt, TcpClientKeepSendOpt, TcpClientPlugin, TcpClientSendResult } from './definitions';



export class TcpClientWeb extends WebPlugin implements TcpClientPlugin {

    async connect(options: TcpClientConnectOpt | string): Promise<TcpClientConnectResultOpt> {
        console.log(options);
        throw new Error('Method not implemented for web platform.');
    }

    async sendOnce(options: TcpClientSendOnceOpt): Promise<TcpClientSendResult> {
        console.log(options);
        throw new Error('Method not implemented for web platform.');
    }

    async keepSend(options: TcpClientKeepSendOpt): Promise<void> {
        console.log(options);
        throw new Error('Method not implemented for web platform.');
    }

    async stopSend(options: TcpClientConnectResultOpt): Promise<void> {
        console.log(options);
        throw new Error('Method not implemented for web platform.');
    }

    async disconnect(options: TcpClientConnectResultOpt): Promise<void> {
        console.log(options);
        throw new Error('Method not implemented for web platform.');
    }

}