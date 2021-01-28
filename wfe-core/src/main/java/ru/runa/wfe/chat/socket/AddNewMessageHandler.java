package ru.runa.wfe.chat.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.websocket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.runa.wfe.chat.ChatMessage;
import ru.runa.wfe.chat.ChatMessageFile;
import ru.runa.wfe.chat.dto.ChatDto;
import ru.runa.wfe.chat.dto.ChatMessageDto;
import ru.runa.wfe.chat.dto.ChatNewMessageDto;
import ru.runa.wfe.chat.logic.ChatLogic;
import ru.runa.wfe.execution.logic.ExecutionLogic;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.User;
import ru.runa.wfe.user.logic.ExecutorLogic;

@Component
public class AddNewMessageHandler implements ChatSocketMessageHandler<ChatNewMessageDto> {

    @Autowired
    private ChatSessionHandler sessionHandler;
    @Autowired
    private ChatLogic chatLogic;
    @Autowired
    private ExecutionLogic executionLogic;
    @Autowired
    private ExecutorLogic executorLogic;

    @Transactional
    @Override
    public void handleMessage(Session session, ChatNewMessageDto dto, User user) throws IOException {
        if (executionLogic.getProcess(user, (Long) session.getUserProperties().get("processId")).isEnded()) {
            return;
        }
        ChatMessage newMessage = new ChatMessage();
        newMessage.setCreateActor(user.getActor());
        newMessage.setText(dto.getMessage());
        newMessage.setQuotedMessageIds(dto.getIdHierarchyMessage());
        newMessage.setCreateDate(new Date(Calendar.getInstance().getTime().getTime()));
        String privateNames = dto.getPrivateNames();
        String[] loginsPrivateTable = privateNames != null ? privateNames.split(";") : new String[0];
        Set<Executor> mentionedExecutors = new HashSet<Executor>();
        searchMentionedExecutor(mentionedExecutors, newMessage, loginsPrivateTable, user, session);
        Collection<Actor> mentionedActors = new HashSet<Actor>();
        for (Executor mentionedExecutor : mentionedExecutors) {
            if (mentionedExecutor instanceof Actor) {
                mentionedActors.add((Actor) mentionedExecutor);
            }
        }
        ChatMessageDto chatMessageDto;
        Boolean isPrivate = dto.isPrivate();
        long processId = Long.parseLong(dto.getProcessId());
        if (dto.getFiles() != null) {
            ArrayList<ChatMessageFile> chatMessageFiles = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : dto.getFiles().entrySet()) {
                ChatMessageFile chatMessageFile = new ChatMessageFile(entry.getKey(), entry.getValue());
                chatMessageFiles.add(chatMessageFile);
            }
            chatMessageDto = chatLogic.saveMessageAndBindFiles(user.getActor(), processId, newMessage, mentionedExecutors,
                    isPrivate, chatMessageFiles);
        } else {
            Long newMessId = chatLogic.saveMessage(user.getActor(), processId, newMessage, mentionedExecutors, isPrivate);
            newMessage.setId(newMessId);
            chatMessageDto = new ChatMessageDto(newMessage);

        }
        sessionHandler.sendNewMessage(mentionedExecutors, chatMessageDto, isPrivate);
    }

    void searchMentionedExecutor(Collection<Executor> mentionedExecutors, ChatMessage newMessage, String[] loginsPrivateTable, User user,
            Session session) {
        // mentioned executors are defined by '@user' pattern
        int dogIndex = -1;
        int spaceIndex = -1;
        String login;
        String searchText = newMessage.getText();
        Executor actor;
        while (true) {
            dogIndex = searchText.indexOf('@', dogIndex + 1);
            if (dogIndex != -1) {
                spaceIndex = searchText.indexOf(' ', dogIndex);
                if (spaceIndex != -1) {
                    login = searchText.substring(dogIndex + 1, spaceIndex);
                } else {
                    login = searchText.substring(dogIndex + 1);
                }
                try {
                    actor = executorLogic.getExecutor(user, login);
                } catch (Exception e) {
                    actor = null;
                }
                if (actor != null) {
                    mentionedExecutors.add(actor);
                }

            } else {
                if ((loginsPrivateTable.length > 0) && (loginsPrivateTable[0].trim().length() != 0)) {
                    for (String loginPrivate : loginsPrivateTable) {
                        actor = executorLogic.getExecutor(user, loginPrivate);
                        if (actor != null) {
                            if (!mentionedExecutors.contains(actor)) {
                                mentionedExecutors.add(actor);
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean isSupports(Class<? extends ChatDto> messageType) {
        return messageType.equals(ChatNewMessageDto.class);
    }
}