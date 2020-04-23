package dev.ramzanzan.chat;

import dev.ramzanzan.chat.model.RimpClientSession;
import dev.ramzanzan.chat.service.RimpServerWebSocketHandler;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import dev.ramzanzan.chat.service.Registry;
import dev.ramzanzan.chat.service.RimpServer;

@SpringBootApplication(scanBasePackages = "dev.ramzanzan.chat.service")
@EnableWebSocket
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }

    @Bean
    public Registry<RimpClientSession> registry(){
        return new Registry<>();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RimpServer server(Registry<RimpClientSession> registry){
        return new RimpServer(registry);
    }


    @Bean
    public WebSocketConfigurer webSocketConfigurer(RimpServerWebSocketHandler rswsh){
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                registry.addHandler(rswsh);
            }
        };
    }


}
