package ru.runa.wfe.chat.logic;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import ru.runa.wfe.chat.ChatMessage;
import ru.runa.wfe.chat.ChatMessageFile;
import ru.runa.wfe.chat.ChatsUserInfo;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.logic.WfCommonLogic;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;

public class ChatLogic extends WfCommonLogic {

    private Properties properties = ClassLoaderUtil.getProperties("chat.properties", true);

    public List<Long> getActiveChatIds(Actor user) {
        List<Long> ret = chatDao.getActiveChatIds(user);
        if (ret == null) {
            ret = new ArrayList<Long>();
        }
        return ret;
    }

    public Set<Executor> getAllUsers(Long processId, Actor user) {
        Set<Executor> ret = new HashSet<Executor>();
        // выбираем таски
        List<Task> tasks = Lists.newArrayList();
        Process process = processDao.getNotNull(processId);
        tasks.addAll(taskDao.findByProcess(process));
        List<Process> subprocesses = nodeProcessDao.getSubprocessesRecursive(process);
        for (Process subprocess : subprocesses) {
            tasks.addAll(taskDao.findByProcess(subprocess));
        }
        // собираем юзеров
        for(Task task : tasks) {
            ret.addAll(executorDao.getActors(new ArrayList<Long>(task.getOpenedByExecutorIds())));
        }
        //
        return ret;
    }

    public List<Long> getNewMessagesCounts(List<Long> chatsIds, List<Boolean> isMentions, Actor user) {
        return chatDao.getNewMessagesCounts(chatsIds, isMentions, user);
    }

    public ChatsUserInfo getUserInfo(Actor actor, Long processId) {
        return chatDao.getUserInfo(actor, processId);
    }

    public long getNewMessagesCount(Long lastMessageId, Long processId) {
        return chatDao.getNewMessagesCount(lastMessageId, processId);
    }

    public void updateUserInfo(Actor actor, Long processId, Long lastMessageId) {
        chatDao.updateUserInfo(actor, processId, lastMessageId);
    }

    public List<ChatMessage> getMessages(Long processId) {
        return chatDao.getAll(processId);
    }

    public ChatMessage getMessage(Long messageId) {
        return chatDao.getMessage(messageId);
    }

    public List<ChatMessage> getMessages(Long processId, Long firstId, int count) {
        return chatDao.getMessages(processId, firstId, count);
    }

    public List<ChatMessage> getFirstMessages(Long processId, int count) {
        return chatDao.getFirstMessages(processId, count);
    }

    public List<ChatMessage> getNewMessages(Long processId, Long lastId) {
        return chatDao.getNewMessages(processId, lastId);
    }

    public long saveMessage(Long processId, ChatMessage message) {
        if (!message.getIsPrivate()) {
            Set<Executor> executors = getAllUsers(processId, message.getCreateActor());
            return chatDao.save(message, executors);
        }
        else {
            return chatDao.save(message);
        }
    }

    public long getAllMessagesCount(Long processId) {
        return chatDao.getMessagesCount(processId);
    }

    public void deleteMessage(Long messId) {
        chatDao.deleteMessageFiles(messId);
        chatDao.deleteMessage(messId);
    }

    public List<ChatMessageFile> getMessageFiles(ChatMessage message) {
        return chatDao.getMessageFiles(message);
    }

    public ChatMessageFile saveFile(ChatMessageFile file) {
        return chatDao.saveFile(file);
    }

    public ChatMessageFile getFile(Long fileId) {
        return chatDao.getFile(fileId);
    }

    public void updateMessage(ChatMessage message) {
        chatDao.updateMessage(message);
    }

    public boolean canEditMessage(Actor user) {
        return true;
    }

    public boolean sendMessageToEmail(String title, String message, String Emaile) {
        // Создаем соединение для отправки почтового сообщения
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(properties,
                // Аутентификатор - объект, который передает логин и пароль
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.getProperty("chat.email.login"), properties.getProperty("chat.email.password"));
                    }
                });
        // Создаем новое почтовое сообщение
        Message mimeMessage = new MimeMessage(session);
        try {
            // От кого
            mimeMessage.setFrom(new InternetAddress(properties.getProperty("chat.email.login")));
            // Кому
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(Emaile));
            // Тема письма
            mimeMessage.setSubject(title);
            // Текст письма
            mimeMessage.setText(message);
            // отправка
            Transport.send(mimeMessage);
        } catch (AddressException e) {
            return false;
        } catch (MessagingException e) {
            return false;
        }
        return true;
    }

}
