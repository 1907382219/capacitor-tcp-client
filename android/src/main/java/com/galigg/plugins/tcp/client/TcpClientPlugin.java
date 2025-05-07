package com.svend.plugins.tcp.socket;

import android.Manifest;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.InetSocketAddress;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * TCP Socket插件
 * 用于在Android端实现TCP Socket通信功能
 */
@CapacitorPlugin(name = "TcpClient", permissions = {
    @Permission(
        alias = "network",
        strings = {Manifest.permission.ACCESS_NETWORK_STATE}
    )
})
public class TcpClientPlugin extends Plugin {

    private class ConnectionCache {
        public ConnectionTask connection;
        public ScheduledFuture sendScheduledTask;
    }

    // 默认缓冲区大小（8KB）
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    // 重新建立连接延迟 ms
    private final long RETRY_DURATION_DURATION = 2340;
    // 历史连接ID
    private int beforeConnectId = 0;

    // 当连接状态发生变化
    private final String ON_CONNECT_STATE_CHANGE = "onConnectStateChange";
    // 当有输入数据
    private final String ON_DATA = "onData";
    // Socket连接实例HashMap
    private final ConcurrentHashMap<Integer, ConnectionCache> connections = new ConcurrentHashMap();

    // 创建一个定时任务线程池，其中参数 1 表示线程池中只维护一个工作线程，这意味着所有的定时任务都会在这一个线程中按顺序执行
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    // 停止发送内容周期任务并删除
    private void deleteSendScheduledTask(int connectId) throws RuntimeException {

        if (connectId == 0) {
            throw new RuntimeException("缺少ConnectID参数");
        }

        ConnectionCache cache = this.connections.get(connectId);

        if (cache == null) {
            throw new RuntimeException("未找到连接实例");
        }

        if (cache.sendScheduledTask != null) {
            cache.sendScheduledTask.cancel(false);
            cache.sendScheduledTask = null;
        }
    }

    // Socket连接实例
    private class ConnectionTask {

        // 当前连接的ID
        public int connectId = 0;
        // 当前Socket连接实例的循环线程（会不停的判定连接和无限重连）
        public ExecutorService socketRuntimeThread = Executors.newSingleThreadExecutor();
        // 当前Socket连接Reader读取内容线程
        public ExecutorService socketReaderThread = Executors.newSingleThreadExecutor();

        // 当前的Socket是否工作
        public Boolean canConnect = false;
        public Boolean canRead = false;
        // 当前Socket连接的IP
        private String ip;
        // 当前Socket连接的Port
        private int port;

        // 当前Socket连接的实例
        private Socket socket;
        // 当前Socket连接的输入流
        private DataInputStream input;
        // 当前Socket连接的输出流
        private DataOutputStream output;

        // 如果没有ConnectID就开启一个新连接实例
        ConnectionTask(String ip, int port) {
            this.canConnect = true;
            this.ip = ip;
            this.port = port;
            this.connectId = ++beforeConnectId;
            this.startConnect();
        }

        private void startConnect() {
            notifyConnectionStateChange(false, "准备建立连接");
            this.socketRuntimeThread.submit(() -> {
                // 检测是否重连
                while (this.canConnect) {
                    // 检测是否重连
                    if (this.socket == null || !this.socket.isConnected() || this.socket.isClosed()) {
                        try {
                            this.disconnect(false);
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, port), 1000);
                            socket.setKeepAlive(true);
                            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                            this.socket = socket;
                            this.input = input;
                            this.output = output;
                            this.canRead = true;
                            this.startReader();
                            Log.e("TcpClientPlugin", ip + ":" + port + "连接成功");

                            // 延迟通知：执行速度太快，外部还没有监听事件还没开始，这边就已经发出了
                            Thread.sleep(150);
                            notifyConnectionStateChange(true, "建立连接成功");
                        }
                        catch (Exception e) {

                            this.disconnect(false);

                            try {
                                Log.e("TcpClientPlugin", "连接失败：" + e.getMessage());
                                Thread.sleep(RETRY_DURATION_DURATION);
                            } catch (Exception retryE) {}

                            continue;
                        }
                    }

                    // 检测是否还在线
                    try {
                        this.output.write(0xff);
                        Log.e("TcpClientPlugin", "心跳保持");
                        Thread.sleep(RETRY_DURATION_DURATION);
                    }
                    catch (Exception e) {
                        try {
                            this.disconnect(false);
                            Log.e("TcpClientPlugin", "发送失败，已断开连接：" + e.getMessage());
                            notifyConnectionStateChange(false, "连接异常，准备重连");
                        } catch (Exception ex) {}
                    }

                }
            });
        }

        private void startReader() {
            // 开始读取数据
            this.socketReaderThread.submit(() -> {
                while (canRead) {
                    try {
                        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                        int readBytes = this.input.read(buffer);
                        if (readBytes == -1) {
                            this.disconnect(false);
                            notifyConnectionStateChange(false, "服务器主动断开连接 null");
                            Log.e("TcpClientPlugin", "读取数据为 null，服务器主动断开连接，继续等待重连");
                            continue;
                        }
                        String value = new String(buffer, 0, readBytes);
                        notifyData(value);
                        Log.e("TcpClientPlugin", "读取数据为：" + value);
                    }
                    catch (IOException e) {
                        this.disconnect(false);
                        notifyConnectionStateChange(false, "读取失败，异常内容：" + e.getMessage());
                        Log.e("TcpClientPlugin", "读取失败，异常内容：" + e.getMessage());
                    }
                }
            });
        }

        private void notifyConnectionStateChange(Boolean connected, String message) {
            JSObject result = new JSObject();
            result.put("connect_id", this.connectId);
            result.put("connected", connected);
            result.put("data", message);
            notifyListeners(ON_CONNECT_STATE_CHANGE, result);
        }

        private void notifyData(String value) {
            JSObject result = new JSObject();
            result.put("connect_id", this.connectId);
            result.put("data", value);
            notifyListeners(ON_DATA, result);
        }

        // 发送数据
        public String send(String value) {
            try {
                Log.e("TCPClientPlugin:Send", "发送数据：" + value);
                if (this.socket.isConnected()) {
                    this.output.write(value.getBytes());
                    this.output.flush();
                    return "发送成功";
                } else {
                    return "发送失败：未建立连接";
                }
            } catch (Exception e) {
                String msg = "发送数据失败：" + e.getMessage();
                Log.e("TCPClientPlugin:Send", msg);
                return "未知错误：" + msg;
            }
        }

        // 关闭所有的连接和流
        public void disconnect(Boolean destroy)  {
            try {
                // 先设置标志位，让读取线程退出循环
                this.canRead = false;
                
                if (this.socket != null) {
                    try {
                        // 使用 setSoTimeout 设置一个较短的超时，强制 readLine() 尽快返回
                        this.socket.setSoTimeout(100);
                    } catch (Exception e) {
                        Log.e("TcpClientPlugin", "设置超时失败: " + e.getMessage());
                    }
                    
                    // 等待一小段时间，让读取线程有机会退出
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {}
                    
                    // 然后再关闭连接
                    try {
                        if (this.output != null) {
                            this.output.close();
                            this.output = null;
                        }
                        
                        if (this.socket != null) {
                            this.socket.close();
                            this.socket = null;
                        }
                        
                        if (this.input != null) {
                            this.input.close();
                            this.input = null;
                        }
                    } catch (Exception e) {
                        Log.e("TcpClientPlugin", "关闭连接异常: " + e.getMessage());
                    }
                }

                deleteSendScheduledTask(this.connectId);

                if (destroy) {
                    this.canConnect = false;

                    if (this.socketReaderThread != null) {
                        this.socketReaderThread.shutdown();
                        this.socketReaderThread = null;
                    }
                    if (this.socketRuntimeThread != null) {
                        this.socketRuntimeThread.shutdown();
                        this.socketRuntimeThread = null;
                    }
                }

            } catch (Exception e) {
                Log.e("TcpClientPlugin", "disconnect 异常" + e.getMessage());
            }
        }

    }

    // 连接
    @PluginMethod()
    public void connect(PluginCall call) {

        try {
            String ip = call.getString("ip");

            if (ip == null) {
                call.reject("connect: 无IP地址");
                return;
            }

            int port = call.getInt("port", 2001);

            ConnectionTask connection = new ConnectionTask(ip, port);
            JSObject result = new JSObject();
            result.put("connect_id", connection.connectId);
            result.put("connected", false);

            ConnectionCache cache = new ConnectionCache();
            cache.connection = connection;
            cache.sendScheduledTask = null;
            this.connections.put(connection.connectId, cache);

            call.resolve(result);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    // 发送一次
    @PluginMethod()
    public void sendOnce(PluginCall call) {
        int connectId = call.getInt("connect_id", 0);
        String data = call.getString("data", "");

        if (connectId == 0) {
            call.reject("缺少ConnectID参数");
            return;
        }

        ConnectionCache cache = this.connections.get(connectId);

        if (cache == null) {
            call.reject("未找到连接实例");
            return;
        }

        try {
            cache.connection.send(data);
            call.resolve();
        } catch (Exception e) {
            call.reject("发送失败：" + e.getMessage());
        }

    }

    // 持续发送
    @PluginMethod()
    public void keepSend(PluginCall call) {
        int connectId = call.getInt("connect_id", 0);
        long duration = call.getInt("duration", 0);
        String data = call.getString("data", "");

        Log.e("TcpClientPlugin", "发送内容：data-" + data + "; connect_id-" + connectId);

        if (connectId == 0) {
            call.reject("缺少ConnectID参数");
            return;
        }

        ConnectionCache cache = this.connections.get(connectId);

        if (cache == null) {
            call.reject("未找到连接实例");
            return;
        }

        if (duration == 0) {
            try {
                cache.connection.send(data);
            } catch (Exception e) {
                call.reject("发送失败：" + e.getMessage());
            }
            return;
        }


        ScheduledFuture repeatSendTask = scheduledExecutor.scheduleWithFixedDelay(
                () -> {
                    try {
                        cache.connection.send(data);
                        Log.e("TcpClientPlugin", "发送内容：data-" + data + "; connect_id-" + connectId + "; duration-" + duration);
                    } catch (Exception e) {
                        call.reject("发送失败，请及时调用 stopSend 方法结束触发：" + e.getMessage());
                    }
                },
                0,  // 初始延迟0秒
                duration,  // 间隔
                TimeUnit.MILLISECONDS  // 时间单位为毫秒
        );

        cache.sendScheduledTask = repeatSendTask;

    }

    // 停止发送内容周期
    @PluginMethod()
    public void stopSend(PluginCall call) {
        int connectId = call.getInt("connect_id", 0);
        try {
            this.deleteSendScheduledTask(connectId);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    // 断开连接
    @PluginMethod()
    public void disconnect(PluginCall call) {
        Log.e("TcpClientPlugin", "断开连接成功 1");
        int connectId = call.getInt("connect_id", 0);
        if (connectId == 0) {
            return;
        }

        Log.e("TcpClientPlugin", "断开连接成功 2");

        ConnectionCache cache = this.connections.get(connectId);
        Log.e("TcpClientPlugin", "断开连接成功 3");
        if (cache == null) {
            call.reject("找不到Socket实例");
            return;
        }
        Log.e("TcpClientPlugin", "断开连接成功 4");

        try {
            Log.e("TcpClientPlugin", "断开连接成功 4.1");
            this.deleteSendScheduledTask(connectId);
            Log.e("TcpClientPlugin", "断开连接成功 4.2");
            cache.connection.disconnect(true);
            Log.e("TcpClientPlugin", "断开连接成功 4.3");
            this.connections.remove(connectId);
            Log.e("TcpClientPlugin", "断开连接成功 5");
            JSObject result = new JSObject();
            result.put("data", "连接 connect_id: "+ connectId + "; ip: " + cache.connection.ip + "成功断开");
            Log.e("TcpClientPlugin", "断开连接成功 6");
            call.resolve(result);
            Log.e("TcpClientPlugin", "断开连接成功");
        } catch (Exception e) {
            Log.e("TcpClientPlugin", "断开连接成功 7");
            call.reject("连接：connectId" + "断开失败；异常：" + e.getMessage());
            Log.e("TcpClientPlugin", "断开连接成功 8");
            Log.e("TcpClientPlugin", "断开失败：" + e.getMessage());
        }

    }

    @Override
    protected void handleOnDestroy() {
        Log.e("TcpClientPlugin", "销毁插件");
        for (ConnectionCache cache : this.connections.values()) {
            try {
                cache.connection.canConnect = cache.connection.canRead = false;
                cache.connection.disconnect(true);
                this.deleteSendScheduledTask(cache.connection.connectId);
            } catch (Exception e) {}
        }
        this.connections.clear();
    }

}
