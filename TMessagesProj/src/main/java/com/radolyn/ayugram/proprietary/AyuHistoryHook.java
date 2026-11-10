/*
 * Ghostgram for Android
 * Enhanced Privacy & Anti-Recall
 */

package com.radolyn.ayugram.proprietary;

import android.util.Pair;
import android.util.SparseArray;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.messages.AyuMessagesController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class AyuHistoryHook {

    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messArr) {
        if (messArr == null || messArr.isEmpty()) {
            return new Pair<>(0, 0);
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < messArr.size(); i++) {
            MessageObject msg = messArr.get(i);
            if (msg != null) {
                int id = msg.getId();
                if (id < min) min = id;
                if (id > max) max = id;
            }
        }
        if (min == Integer.MAX_VALUE) {
            return new Pair<>(0, 0);
        }
        return new Pair<>(min, max);
    }

    public static void doHook(int currentAccount, ArrayList<MessageObject> messArr, SparseArray<MessageObject>[] messagesDict, int startId, int endId, long dialogId, int limit, long topicId, boolean isSecretChat) {
        if (messArr == null || isSecretChat) {
            return;
        }

        try {
            long userId = UserConfig.getInstance(currentAccount).getClientUserId();
            List<DeletedMessageFull> deleted = AyuMessagesController.getInstance().getMessages(userId, dialogId, topicId, startId, endId, limit);
            if (deleted == null || deleted.isEmpty()) {
                return;
            }

            for (DeletedMessageFull item : deleted) {
                if (item == null || item.message == null) {
                    continue;
                }
                int mid = item.message.messageId;
                if (messagesDict != null && messagesDict.length > 0 && messagesDict[0] != null && messagesDict[0].indexOfKey(mid) >= 0) {
                    continue;
                }

                TLRPC.TL_message msg = new TLRPC.TL_message();
                msg.id = mid;
                msg.date = item.message.date;
                msg.message = item.message.text != null ? item.message.text : "";
                msg.edit_date = item.message.editDate;
                msg.views = item.message.views;
                msg.flags = item.message.flags;
                msg.dialog_id = item.message.dialogId;
                msg.grouped_id = item.message.groupedId;

                if (item.message.peerId < 0) {
                    msg.peer_id = new TLRPC.TL_peerChannel();
                    msg.peer_id.channel_id = -item.message.peerId;
                } else {
                    msg.peer_id = new TLRPC.TL_peerUser();
                    msg.peer_id.user_id = item.message.peerId;
                }

                if (item.message.fromId < 0) {
                    msg.from_id = new TLRPC.TL_peerChannel();
                    msg.from_id.channel_id = -item.message.fromId;
                } else {
                    msg.from_id = new TLRPC.TL_peerUser();
                    msg.from_id.user_id = item.message.fromId;
                }

                MessageObject obj = new MessageObject(currentAccount, msg, false, true);
                obj.deleted = true;

                int insertIndex = 0;
                while (insertIndex < messArr.size() && messArr.get(insertIndex).getId() > mid) {
                    insertIndex++;
                }
                messArr.add(insertIndex, obj);
                if (messagesDict != null && messagesDict.length > 0 && messagesDict[0] != null) {
                    messagesDict[0].put(mid, obj);
                }
            }
        } catch (Exception e) {
            // Safe fallback
        }
    }
}
