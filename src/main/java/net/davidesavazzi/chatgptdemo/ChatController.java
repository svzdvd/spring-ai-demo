package net.davidesavazzi.chatgptdemo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient.Builder builder;

    @Autowired
    public ChatController(ChatClient.Builder builder) {
        this.builder = builder;
    }

    @GetMapping("/chat")
    public String generate(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return builder.build().prompt(message).call().content();
    }
}
