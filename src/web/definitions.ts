// src/definitions.ts
import type { PluginListenerHandle } from '@capacitor/core';

export interface TcpClientConnectOpt {
    ip: string;
    port: number;
}

export interface TcpClientConnectResultOpt {
    connect_id: number;
    connected: boolean;
    data: string;
};

interface TcpClientBaseSendOpt {
    connect_id: number;
    data: string;
}

export type TcpClientStateOpt = TcpClientConnectResultOpt;

export interface TcpClientStopSendOpt extends Pick<TcpClientConnectResultOpt, 'connect_id'> {};

export interface TcpClientDisconnectOpt extends Pick<TcpClientConnectResultOpt, 'connect_id'> {};

export type TcpClientSendOnceOpt = TcpClientBaseSendOpt;

export interface TcpClientKeepSendOpt extends TcpClientBaseSendOpt {
    duration?: number;
}

export interface TcpClientDataEvent {
    connect_id: number;
    data: string;
}

export interface TcpClientSendResult {
    connect_id: number;
    success: boolean;
    message: string;
}
    

export interface TcpClientPlugin {
    
    connect(options: TcpClientConnectOpt): Promise<TcpClientConnectResultOpt>;

    sendOnce(options: TcpClientSendOnceOpt): Promise<TcpClientSendResult>;

    keepSend(options: TcpClientKeepSendOpt): Promise<void>;

    stopSend(options: TcpClientStopSendOpt): Promise<void>;

    disconnect(options: TcpClientDisconnectOpt): Promise<void>;

    addListener(
        eventName: 'onData',
        listenerFunc: (data: TcpClientDataEvent) => void
    ): Promise<PluginListenerHandle>;

    addListener(
        eventName: 'onConnectStateChange',
        listenerFunc: (data: TcpClientStateOpt) => void
    ): Promise<PluginListenerHandle>;

}