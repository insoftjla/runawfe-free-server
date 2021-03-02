package ru.runa.wfe.chat;

import ru.runa.wfe.definition.Deployment;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.presentation.ClassPresentation;
import ru.runa.wfe.presentation.DefaultDbSource;
import ru.runa.wfe.presentation.FieldDescriptor;
import ru.runa.wfe.presentation.FieldFilterMode;
import ru.runa.wfe.security.Permission;

/**
 * Created on 21.02.2021
 *
 * @author Sergey Inyakin
 */
public class ChatRoomClassPresentation extends ClassPresentation {
    public static final String PROCESS_ID = "batch_presentation.process.id";
    public static final String DEFINITION_NAME = "batch_presentation.process.definition_name";
    public static final String NEW_MESSAGES = "chat_rooms.new_messages";

    private static final ClassPresentation INSTANCE = new ChatRoomClassPresentation();

    private static class ProcessDbSource extends DefaultDbSource {

        public ProcessDbSource(Class<?> sourceObject, String valueDBPath) {
            super(sourceObject, valueDBPath);
        }

        @Override
        public String getValueDBPath(AccessType accessType, String alias) {
            return alias + "." + valueDBPath;
        }

        @Override
        public String getJoinExpression(String alias) {
            return alias + ".message.process";
        }

        @Override
        public String getGroupByExpression(String alias) {
            return alias +  "." +valueDBPath;
        }
    }

    private static class DeploymentDbSource extends DefaultDbSource {

        public DeploymentDbSource(Class<?> sourceObject, String valueDBPath) {
            super(sourceObject, valueDBPath);
        }

        @Override
        public String getValueDBPath(AccessType accessType, String alias) {
            return alias + "." + valueDBPath;
        }

        @Override
        public String getJoinExpression(String alias) {
            return alias + ".message.process.deployment";
        }

        @Override
        public String getGroupByExpression(String alias) {
            return alias + "." + valueDBPath;
        }
    }

    private static class ChatMessageRecipientDbSource extends DefaultDbSource {

        public ChatMessageRecipientDbSource(Class<?> sourceObject, String valueDBPath) {
            super(sourceObject, valueDBPath);
        }

        @Override
        public String getValueDBPath(AccessType accessType, String alias) {
            return "count(" + alias + "." + valueDBPath + ")";
        }
    }

    private static class ChatMessageDbSource extends DefaultDbSource {

        public ChatMessageDbSource(Class<?> sourceObject, String valueDBPath) {
            super(sourceObject, valueDBPath);
        }

        @Override
        public String getValueDBPath(AccessType accessType, String alias) {
            return "";
        }

        @Override
        public String getJoinExpression(String alias) {
            return alias + ".message";
        }
    }

    private ChatRoomClassPresentation() {
        super(ChatMessageRecipient.class, "", true, new FieldDescriptor[]{
                new FieldDescriptor(PROCESS_ID, Long.class.getName(), new ChatMessageDbSource(ChatMessage.class, ""), true, FieldFilterMode.DATABASE,
                        "ru.runa.common.web.html.PropertyTdBuilder", new Object[]{ Permission.READ, "id" }),
                new FieldDescriptor(PROCESS_ID, Long.class.getName(), new ProcessDbSource(Process.class, "id"), true, FieldFilterMode.DATABASE,
                        "ru.runa.common.web.html.PropertyTdBuilder", new Object[]{ Permission.READ, "id" }),
                new FieldDescriptor(DEFINITION_NAME, String.class.getName(), new DeploymentDbSource(Deployment.class, "name"), true,
                        FieldFilterMode.DATABASE, "ru.runa.common.web.html.PropertyTdBuilder", new Object[]{ Permission.READ, "processName" }),
                new FieldDescriptor(NEW_MESSAGES, Integer.class.getName(), new ChatMessageRecipientDbSource(ChatMessageRecipient.class, "readDate"), true,
                        FieldFilterMode.DATABASE, "ru.runa.wf.web.html.ChatNewMessagesCountTdBuilder", new Object[]{ Permission.READ, "processId" }) });
    }

    public static ClassPresentation getInstance() {
        return INSTANCE;
    }
}
