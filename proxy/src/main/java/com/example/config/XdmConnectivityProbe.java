package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * 全局远程模式（mock-mode=false 且 remote-entities 为空）时，启动阶段探测 xDM-F 端口；
 * 不可达则自动改为 Mock，避免评委/演示环境未开 8003 时整站不可用。
 */
@Component
@Order(0)
public class XdmConnectivityProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(XdmConnectivityProbe.class);
    private static final int CONNECT_TIMEOUT_MS = 2000;

    private final XdmProxyProperties props;

    public XdmConnectivityProbe(XdmProxyProperties props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.isMockMode()) {
            log.info("xDM: mock-mode 已开启，使用模拟数据（不依赖本机 8003）");
            return;
        }
        if (!props.getRemoteEntityList().isEmpty()) {
            log.info("xDM: 已配置 remote-entities，仅白名单实体走远程；不做 8003 连通性自动降级");
            return;
        }
        if (reachableXdmPort()) {
            log.info("xDM: 检测到 xDM-F 端口可访问，使用远程数据");
            return;
        }
        log.warn("xDM: 未检测到 xDM-F（8003）服务，已自动切换为 Mock 模式");
        props.setMockMode(true);
    }

    private boolean reachableXdmPort() {
        try {
            URI uri = URI.create(props.getBaseUrl());
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || host.isEmpty()) {
                return false;
            }
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
