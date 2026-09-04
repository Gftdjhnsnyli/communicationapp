package com.example.communicationapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {ConversationEntity.class, MessageEntity.class, SmsPartEntity.class},
        version = 2,
        exportSchema = true)
public abstract class CommunicationDatabase extends RoomDatabase {
    private static volatile CommunicationDatabase instance;

    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE messages ADD COLUMN dispatchState INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE messages ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE messages ADD COLUMN subscriptionId INTEGER");
            database.execSQL("ALTER TABLE messages ADD COLUMN incomingFingerprint TEXT");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_messages_incomingFingerprint " +
                    "ON messages(incomingFingerprint)");
        }
    };

    public static CommunicationDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CommunicationDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CommunicationDatabase.class,
                            "communication.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
