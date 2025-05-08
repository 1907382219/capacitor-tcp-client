# Capacitor TCP Client Android

A Capacitor plugin for TCP client functionality on Android platform.

一个用于 Android 平台的 Capacitor TCP 客户端插件。

## Installation | 安装

```bash
npm install capacitor-tcp-client-android
```

## Usage | 使用方法

```typescript
import { TcpClient } from 'capacitor-tcp-client-android';

// Connect to a TCP server
// 连接到 TCP 服务器
const result = await TcpClient.connect({
  ip: '127.0.0.1',
  port: 8080
});

// Send data once
// 发送一次数据
await TcpClient.sendOnce({
  connect_id: result.connect_id,
  data: 'Hello, Server!'
});

// Keep sending data
// 持续发送数据
await TcpClient.keepSend({
  connect_id: result.connect_id,
  data: 'Hello, Server!',
  duration: 1000 // 发送间隔，单位毫秒
});

// Stop sending data
// 停止发送数据
await TcpClient.stopSend({
  connect_id: result.connect_id
});

// Disconnect
// 断开连接
await TcpClient.disconnect({
  connect_id: result.connect_id
});

// Add listeners
// 添加监听器
TcpClient.addListener('onData', (data) => {
  console.log('Received data:', data);
});

TcpClient.addListener('onConnectStateChange', (state) => {
  console.log('Connection state changed:', state);
});
```

## API | 接口说明

### connect(options: TCPClientConnectOpt): Promise<TCPClientConnectResultOpt>

Connect to a TCP server.

连接到 TCP 服务器。

参数说明：
- ip: 服务器 IP 地址
- port: 服务器端口号

返回值：
- connect_id: 连接 ID，用于后续操作
- connected: 连接状态
- data: 连接信息

### sendOnce(options: TCPClientSendOnceOpt): Promise<TcpClientSendResult>

Send data once to the connected server.

向已连接的服务器发送一次数据。

参数说明：
- connect_id: 连接 ID
- data: 要发送的数据内容

返回值：
- connect_id: 连接 ID，用于后续操作
- success: 是否发送成功
- message: 发送结果描述

### keepSend(options: TCPClientKeepSendOpt): Promise<void>

Keep sending data to the connected server at specified intervals.

以指定间隔持续向服务器发送数据。

参数说明：
- connect_id: 连接 ID
- data: 要发送的数据内容
- duration: 发送间隔时间（毫秒），可选参数

### stopSend(options: TCPClientStopSendOpt): Promise<void>

Stop the continuous sending of data.

停止持续发送数据。

参数说明：
- connect_id: 连接 ID

### disconnect(options: TCPClientDisconnectOpt): Promise<void>

Disconnect from the server.

断开与服务器的连接。

参数说明：
- connect_id: 连接 ID

### addListener(eventName: 'onData', listenerFunc: (data: TCPClientDataEvent) => void): Promise<PluginListenerHandle>

Add a listener for data events.

添加数据事件监听器。

### addListener(eventName: 'onConnectStateChange', listenerFunc: (data: TCPClientStateOpt) => void): Promise<PluginListenerHandle>

Add a listener for connection state changes.

添加连接状态变化监听器。

## Features | 特性

- 支持 TCP 连接建立和断开
- 支持单次发送和持续发送数据
- 支持自动重连机制
- 支持多连接管理
- 支持心跳检测
- 支持自定义发送间隔
- 支持事件监听

## License | 许可证

MIT

## Author | 作者

GaliGG_CC