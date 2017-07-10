package com.ironz.binaryprefs;

import android.content.Context;

import com.ironz.binaryprefs.cache.CacheProvider;
import com.ironz.binaryprefs.cache.ConcurrentCacheProviderImpl;
import com.ironz.binaryprefs.encryption.ByteEncryption;
import com.ironz.binaryprefs.events.BroadcastEventBridgeImpl;
import com.ironz.binaryprefs.events.EventBridge;
import com.ironz.binaryprefs.exception.ExceptionHandler;
import com.ironz.binaryprefs.file.adapter.FileAdapter;
import com.ironz.binaryprefs.file.adapter.NioFileAdapter;
import com.ironz.binaryprefs.file.directory.AndroidDirectoryProviderImpl;
import com.ironz.binaryprefs.file.directory.DirectoryProvider;
import com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.ironz.binaryprefs.file.transaction.MultiProcessTransactionImpl;
import com.ironz.binaryprefs.lock.LockFactory;
import com.ironz.binaryprefs.lock.SimpleLockFactoryImpl;
import com.ironz.binaryprefs.serialization.SerializerFactory;
import com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.ironz.binaryprefs.serialization.serializer.persistable.PersistableRegistry;
import com.ironz.binaryprefs.task.ScheduledBackgroundTaskExecutor;
import com.ironz.binaryprefs.task.TaskExecutor;

public final class BinaryPreferencesBuilder {

    private static final String DEFAULT_NAME = "default";
    private final Context context;
    private final PersistableRegistry persistableRegistry = new PersistableRegistry();
    private ByteEncryption byteEncryption = ByteEncryption.NO_OP;
    private String name = DEFAULT_NAME;
    private ExceptionHandler exceptionHandler = ExceptionHandler.IGNORE;

    public BinaryPreferencesBuilder(Context context) {
        this.context = context;
    }

    public BinaryPreferencesBuilder encryption(ByteEncryption encryption) {
        this.byteEncryption = encryption;
        return this;
    }

    public BinaryPreferencesBuilder exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public BinaryPreferencesBuilder name(String name) {
        this.name = name;
        return this;
    }

    public BinaryPreferencesBuilder registerPersistableByKey(String token, Class<? extends Persistable> persistable) {
        persistableRegistry.register(token, persistable);
        return this;
    }

    public Preferences build() {
        DirectoryProvider directoryProvider = new AndroidDirectoryProviderImpl(context, name);
        FileAdapter fileAdapter = new NioFileAdapter(directoryProvider);
        LockFactory lockFactory = new SimpleLockFactoryImpl(name, directoryProvider);
        FileTransaction fileTransaction = new MultiProcessTransactionImpl(fileAdapter, lockFactory);
        CacheProvider cacheProvider = new ConcurrentCacheProviderImpl(name);
        TaskExecutor executor = new ScheduledBackgroundTaskExecutor(exceptionHandler);
        SerializerFactory serializerFactory = new SerializerFactory(persistableRegistry);
        EventBridge eventsBridge = new BroadcastEventBridgeImpl(context, name, cacheProvider, serializerFactory, executor, byteEncryption);
        return new BinaryPreferences(fileTransaction, byteEncryption, eventsBridge, cacheProvider, executor, serializerFactory, lockFactory);
    }
}
