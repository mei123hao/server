# 安卓端客户端及服务端socket通信demo
## 测试机型
荣耀8(Android 7.0)  
荣耀Magic2(Harmony OS2)

## 实现功能
* 客户端及服务端的连接
* 客户端向服务端发送短消息
* 客户端向服务端发送客户端的手机视频

## 存在问题
* socket无法保持长连接，不同activity切换存在socket断开的问题，现在暂时通过重连的方式维持，后面可以将socket做成service的形式可以解决这问题
* 服务端接收信息存在信息串扰，信息接收模式如果没有切换的话，客户端如果发送的是视频，信息接收框可能会收到大量乱码

Note:此代码仅供学习

[客户端代码](https://github.com/mei123hao/client.git)
