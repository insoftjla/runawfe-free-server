package ru.runa.wfe.chat.socket;

import java.io.IOException;
import javax.websocket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.runa.wfe.chat.ChatMessage;
import ru.runa.wfe.chat.dto.request.EditMessageRequest;
import ru.runa.wfe.chat.dto.broadcast.MessageEditedBroadcast;
import ru.runa.wfe.chat.dto.request.MessageRequest;
import ru.runa.wfe.chat.logic.ChatLogic;
import ru.runa.wfe.execution.logic.ExecutionLogic;
import ru.runa.wfe.user.User;

@Component
public class EditMessageHandler implements ChatSocketMessageHandler<EditMessageRequest, MessageEditedBroadcast> {

    @Autowired
    private ChatSessionHandler sessionHandler;
    @Autowired
    private ChatLogic chatLogic;
    @Autowired
    private ExecutionLogic executionLogic;

    @Transactional
    @Override
    public MessageEditedBroadcast handleMessage(Session session, EditMessageRequest dto, User user) throws IOException {
        if (executionLogic.getProcess(user, dto.getProcessId()).isEnded()) {
            return null;
        }
        MessageEditedBroadcast broadcast = null;
        ChatMessage newMessage = chatLogic.getMessageById(user, dto.getEditMessageId());
        if (newMessage != null) {
            newMessage.setText(dto.getMessage());
            chatLogic.updateMessage(user, newMessage);
            broadcast = new MessageEditedBroadcast(dto.getEditMessageId(), dto.getMessage());
            sessionHandler.sendMessage(broadcast);
        }
        return broadcast;
    }

    @Override
    public boolean isSupports(Class<? extends MessageRequest> messageType) {
        return messageType.equals(EditMessageRequest.class);
    }
}
