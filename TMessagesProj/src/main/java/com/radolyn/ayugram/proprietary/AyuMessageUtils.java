/*
 * Ghostgram for Android
 * Enhanced Privacy & Anti-Recall
 */

package com.radolyn.ayugram.proprietary;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.entities.AyuMessageBase;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuSavePreferences;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;

public class AyuMessageUtils {

    public static void map(AyuSavePreferences prefs, AyuMessageBase entity) {
        if (prefs == null || entity == null) {
            return;
        }
        TLRPC.Message msg = prefs.getMessage();
        entity.userId = prefs.getUserId();
        entity.dialogId = prefs.getDialogId();
        entity.topicId = prefs.getTopicId();
        entity.messageId = prefs.getMessageId();
        entity.entityCreateDate = prefs.getRequestCatchTime();

        if (msg == null) {
            return;
        }

        entity.date = msg.date;
        entity.flags = msg.flags;
        entity.editDate = msg.edit_date;
        entity.views = msg.views;
        entity.text = msg.message;
        entity.groupedId = msg.grouped_id;

        if (msg.peer_id != null) {
            if (msg.peer_id.channel_id != 0) {
                entity.peerId = -msg.peer_id.channel_id;
            } else if (msg.peer_id.chat_id != 0) {
                entity.peerId = -msg.peer_id.chat_id;
            } else {
                entity.peerId = msg.peer_id.user_id;
            }
        }
        if (msg.from_id != null) {
            if (msg.from_id.channel_id != 0) {
                entity.fromId = -msg.from_id.channel_id;
            } else if (msg.from_id.chat_id != 0) {
                entity.fromId = -msg.from_id.chat_id;
            } else {
                entity.fromId = msg.from_id.user_id;
            }
        }

        if (msg.fwd_from != null) {
            entity.fwdFlags = msg.fwd_from.flags;
            entity.fwdDate = msg.fwd_from.date;
            entity.fwdName = msg.fwd_from.from_name;
            entity.fwdPostAuthor = msg.fwd_from.post_author;
            if (msg.fwd_from.from_id != null) {
                if (msg.fwd_from.from_id.channel_id != 0) {
                    entity.fwdFromId = -msg.fwd_from.from_id.channel_id;
                } else if (msg.fwd_from.from_id.chat_id != 0) {
                    entity.fwdFromId = -msg.fwd_from.from_id.chat_id;
                } else {
                    entity.fwdFromId = msg.fwd_from.from_id.user_id;
                }
            }
        }

        if (msg.reply_to != null) {
            entity.replyFlags = msg.reply_to.flags;
            entity.replyMessageId = msg.reply_to.reply_to_msg_id;
            entity.replyTopId = msg.reply_to.reply_to_top_id;
            entity.replyForumTopic = msg.reply_to.forum_topic;
            if (msg.reply_to.reply_to_peer_id != null) {
                if (msg.reply_to.reply_to_peer_id.channel_id != 0) {
                    entity.replyPeerId = -msg.reply_to.reply_to_peer_id.channel_id;
                } else if (msg.reply_to.reply_to_peer_id.chat_id != 0) {
                    entity.replyPeerId = -msg.reply_to.reply_to_peer_id.chat_id;
                } else {
                    entity.replyPeerId = msg.reply_to.reply_to_peer_id.user_id;
                }
            }
        }

        if (msg.entities != null && !msg.entities.isEmpty()) {
            try {
                NativeByteBuffer byteBuffer = new NativeByteBuffer(true);
                byteBuffer.writeInt32(0x1cb5c415);
                byteBuffer.writeInt32(msg.entities.size());
                for (TLRPC.MessageEntity e : msg.entities) {
                    e.serializeToStream(byteBuffer);
                }
                entity.textEntities = new byte[byteBuffer.length()];
                byteBuffer.position(0);
                byteBuffer.readBytes(entity.textEntities, false);
                byteBuffer.reuse();
            } catch (Exception ignored) {
            }
        }
    }

    public static void mapMedia(AyuSavePreferences prefs, AyuMessageBase entity, boolean force) {
        if (prefs == null || entity == null) {
            return;
        }
        TLRPC.Message msg = prefs.getMessage();
        if (msg == null || msg.media == null) {
            entity.documentType = AyuConstants.DOCUMENT_TYPE_NONE;
            return;
        }

        if (msg.media instanceof TLRPC.TL_messageMediaPhoto) {
            entity.documentType = AyuConstants.DOCUMENT_TYPE_PHOTO;
            File path = FileLoader.getInstance(prefs.getAccountId()).getPathToMessage(msg);
            if (path != null && path.exists()) {
                entity.mediaPath = path.getAbsolutePath();
            }
        } else if (msg.media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.TL_messageMediaDocument docMedia = (TLRPC.TL_messageMediaDocument) msg.media;
            if (MessageObject.isStickerDocument(docMedia.document)) {
                entity.documentType = AyuConstants.DOCUMENT_TYPE_STICKER;
            } else {
                entity.documentType = AyuConstants.DOCUMENT_TYPE_FILE;
            }
            if (docMedia.document != null) {
                entity.mimeType = docMedia.document.mime_type;
                try {
                    NativeByteBuffer byteBuffer = new NativeByteBuffer(docMedia.document.getObjectSize());
                    docMedia.document.serializeToStream(byteBuffer);
                    entity.documentSerialized = new byte[byteBuffer.length()];
                    byteBuffer.position(0);
                    byteBuffer.readBytes(entity.documentSerialized, false);
                    byteBuffer.reuse();
                } catch (Exception ignored) {
                }
            }
            File path = FileLoader.getInstance(prefs.getAccountId()).getPathToMessage(msg);
            if (path != null && path.exists()) {
                entity.mediaPath = path.getAbsolutePath();
            }
        }
    }

    public static void map(EditedMessage entity, TLRPC.Message msg, int currentAccount) {
        if (entity == null || msg == null) {
            return;
        }
        msg.id = entity.messageId;
        msg.date = entity.date;
        msg.message = entity.text != null ? entity.text : "";
        msg.edit_date = entity.editDate;
        msg.views = entity.views;
        msg.flags = entity.flags;
        msg.dialog_id = entity.dialogId;
        msg.grouped_id = entity.groupedId;

        if (entity.peerId < 0) {
            msg.peer_id = new TLRPC.TL_peerChannel();
            msg.peer_id.channel_id = -entity.peerId;
        } else {
            msg.peer_id = new TLRPC.TL_peerUser();
            msg.peer_id.user_id = entity.peerId;
        }

        if (entity.fromId < 0) {
            msg.from_id = new TLRPC.TL_peerChannel();
            msg.from_id.channel_id = -entity.fromId;
        } else {
            msg.from_id = new TLRPC.TL_peerUser();
            msg.from_id.user_id = entity.fromId;
        }

        if (entity.textEntities != null && entity.textEntities.length > 0) {
            try {
                NativeByteBuffer byteBuffer = new NativeByteBuffer(entity.textEntities.length);
                byteBuffer.writeBytes(entity.textEntities);
                byteBuffer.position(0);
                byteBuffer.readInt32(false); // magic
                int count = byteBuffer.readInt32(false);
                msg.entities = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    msg.entities.add(TLRPC.MessageEntity.TLdeserialize(byteBuffer, byteBuffer.readInt32(false), false));
                }
                byteBuffer.reuse();
            } catch (Exception ignored) {
            }
        }
    }

    public static void mapMedia(EditedMessage entity, TLRPC.Message msg) {
        if (entity == null || msg == null) {
            return;
        }
        if (entity.documentType == AyuConstants.DOCUMENT_TYPE_STICKER || entity.documentType == AyuConstants.DOCUMENT_TYPE_FILE) {
            if (entity.documentSerialized != null && entity.documentSerialized.length > 0) {
                try {
                    NativeByteBuffer byteBuffer = new NativeByteBuffer(entity.documentSerialized.length);
                    byteBuffer.writeBytes(entity.documentSerialized);
                    byteBuffer.position(0);
                    TLRPC.Document doc = TLRPC.Document.TLdeserialize(byteBuffer, byteBuffer.readInt32(false), false);
                    byteBuffer.reuse();
                    if (doc != null) {
                        TLRPC.TL_messageMediaDocument media = new TLRPC.TL_messageMediaDocument();
                        media.document = doc;
                        media.flags = 1;
                        msg.media = media;
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
