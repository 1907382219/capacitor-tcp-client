export interface TCPClientPlugin {
  connect(options: { host: string; port: number }): Promise<{ success: boolean }>;
  send(options: { data: string }): Promise<{ success: boolean }>;
  disconnect(): Promise<{ success: boolean }>;
} 