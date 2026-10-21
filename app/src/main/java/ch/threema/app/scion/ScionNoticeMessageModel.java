package ch.threema.app.scion;

import java.time.Instant;

import ch.threema.storage.models.AbstractMessageModel;
import ch.threema.storage.models.MessageState;
import ch.threema.storage.models.MessageType;

/**
 * Dummy model for the notice at the beginning of a chat that its messages go over SCION.
 * It is only shown in ComposeMessageAdapter and never stored.
 */
public class ScionNoticeMessageModel extends AbstractMessageModel {
    private final String text;

    /**
     * @param createdAt the date of the first message, which the sticky date header shows for the notice
     */
    public ScionNoticeMessageModel(String text, Instant createdAt) {
        this.text = text;
        setCreatedAt(createdAt);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getUid() {
        return null;
    }

    @Override
    public boolean isStatusMessage() {
        return true;
    }

    @Override
    public String getIdentity() {
        return null;
    }

    @Override
    public boolean isOutbox() {
        return false;
    }

    @Override
    public MessageType getType() {
        return MessageType.STATUS;
    }

    @Override
    public String getBody() {
        return text;
    }

    @Override
    public boolean isRead() {
        return true;
    }

    @Override
    public boolean isSaved() {
        return false;
    }

    @Override
    public MessageState getState() {
        return null;
    }

    @Override
    public Instant getModifiedAt() {
        return null;
    }

    @Override
    public Instant getPostedAt() {
        return null;
    }

    @Override
    public String getApiMessageId() {
        return null;
    }
}
