package com.hisu.zola.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hisu.zola.database.dao.ContactUserDAO;
import com.hisu.zola.database.dao.ConversationDAO;
import com.hisu.zola.database.dao.MessageDAO;
import com.hisu.zola.database.dao.UserDAO;
import com.hisu.zola.database.entity.ContactUser;
import com.hisu.zola.database.entity.Conversation;
import com.hisu.zola.database.entity.Message;
import com.hisu.zola.database.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {
        User.class, Message.class, Conversation.class, ContactUser.class
}, version = 1, exportSchema = false)
public abstract class Database extends RoomDatabase {
    public static final String DB_NAME = "Zola_Local_DB";
    private static volatile Database INSTANCE;
    /**
     * NUMBER_OF_THREADS is for 4 simple operations: Insert, update, delete & read
     */
    private static final int NUMBER_OF_THREADS = 4;

    public abstract UserDAO userDAO();
    public abstract MessageDAO messageDAO();
    public abstract ConversationDAO conversationDAO();
    public abstract ContactUserDAO contactUserDAO();

    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized Database getDatabase(Context context) {
        if (INSTANCE == null)
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(), Database.class, DB_NAME
            ).build();

        return INSTANCE;
    }
}