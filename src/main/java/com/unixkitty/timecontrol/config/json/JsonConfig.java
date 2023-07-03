package com.unixkitty.timecontrol.config.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class JsonConfig implements AutoCloseable
{
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);

    private final String fileName;
    private final Logger log = LogManager.getLogger(JsonConfig.class);
    private final ConcurrentHashMap<String, Value<?>> config = new ConcurrentHashMap<>();
    private final ObjectObjectImmutablePair<File, File> filePair;
    private final ExecutorService executor = makeIoExecutor();
    private final ExecutorService fileWatcherExecutor = makeIoExecutor();
    @Nullable
    private final ConfigFileWatcher fileWatcher;

    @NotNull
    private State state = State.UNINITIALIZED;

    public JsonConfig(String fileName)
    {
        if (fileName.contains(File.separator))
        {
            throw new IllegalArgumentException("File name must not contain directory separators! " + fileName);
        }

        String jsonExt = ".json";

        if (!fileName.endsWith(jsonExt))
        {
            fileName += jsonExt;
        }

        String dirPath = FabricLoader.getInstance().getConfigDir().toAbsolutePath().toString();

        this.fileName = fileName;
        this.filePair = new ObjectObjectImmutablePair<>(new File(dirPath), FileUtils.getFile(dirPath, this.fileName));

        ConfigFileWatcher configFileWatcher = null;

        try
        {
            configFileWatcher = new ConfigFileWatcher(this);
        }
        catch (IOException e)
        {
            fail("Failed to create file watcher for " + this.fileName, e);
        }

        this.fileWatcher = configFileWatcher;

        Runtime.getRuntime().addShutdownHook(new Thread(this::close, JsonConfig.class.getSimpleName() + " Shutdown Thread"));
    }

    @Override
    public void close()
    {
        if (this.fileWatcherExecutor.isShutdown() && this.executor.isShutdown()) return;

        this.log.info("Shutting down {} watchers", this.fileName);

        if (this.fileWatcher != null)
        {
            this.fileWatcher.stop();
            shutdownExecutor(this.fileWatcherExecutor);
        }

        shutdownExecutor(this.executor);

        this.log.info("Finished shutting down {} watchers", this.fileName);
    }

    public File getFile()
    {
        return this.filePair.right();
    }

    public boolean isReady()
    {
        return this.state != State.UNINITIALIZED;
    }

    public <V> Value<V> defineValue(String key, V defaultValue)
    {
        return new Value<>(key, defaultValue);
    }

    public BooleanValue defineValue(String key, Boolean defaultValue)
    {
        return new BooleanValue(key, defaultValue);
    }

    public IntValue defineInRange(String key, int defaultValue, int min, int max)
    {
        return (IntValue) checkRange(new IntValue(key, defaultValue, min, max), defaultValue);
    }

    public LongValue defineInRange(String key, long defaultValue, long min, long max)
    {
        return (LongValue) checkRange(new LongValue(key, defaultValue, min, max), defaultValue);
    }

    public DoubleValue defineInRange(String key, double defaultValue, double min, double max)
    {
        return (DoubleValue) checkRange(new DoubleValue(key, defaultValue, min, max), defaultValue);
    }

    public FloatValue defineInRange(String key, float defaultValue, float min, float max)
    {
        return (FloatValue) checkRange(new FloatValue(key, defaultValue, min, max), defaultValue);
    }

    public void load()
    {
        fileOperation(false);
    }

    public void save()
    {
        fileOperation(true);
    }

    private void fileOperation(boolean save)
    {
        Runnable operation = () ->
        {
            setScheduledReload(true);

            if (save)
            {
                try
                {
                    _save();
                }
                catch (Exception e)
                {
                    fail("Failed to save config!", e);
                }
            }
            else
            {
                try
                {
                    if (!FileUtils.directoryContains(this.filePair.left(), this.filePair.right()))
                    {
                        _save();
                    }

                    _read();
                }
                catch (Exception e)
                {
                    fail("Error loading config!", e);
                }
            }

            setScheduledReload(false);
        };

        this.executor.submit(operation);
    }

    private void setScheduledReload(boolean state)
    {
        if (this.fileWatcher != null)
        {
            this.fileWatcher.setScheduledReload(state);
        }
    }

    private <N extends Number> NumberValue<N> checkRange(NumberValue<N> numberValue, N value)
    {
        if (numberValue.isValueOutsideRange(value))
        {
            throw new IllegalArgumentException(getOutOfRangeMessage(value, numberValue));
        }

        return numberValue;
    }

    private <N extends Number> String getOutOfRangeMessage(N value, NumberValue<N> numberValue)
    {
        return "Value in " + this.fileName + " for " + numberValue.getName() + " is outside of specified range: " + value + " (" + numberValue.min + ", " + numberValue.max + ")";
    }

    private void _save() throws Exception
    {
        logDebugFileOperation("Saving");

        if (this.config.isEmpty())
        {
            throw new IllegalStateException("No config values to save! key/value map is empty for " + this.fileName);
        }

        try (PrintWriter writer = new PrintWriter(this.filePair.right()))
        {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();

            for (String key : this.config.keySet())
            {
                var value = this.config.get(key);

                if (value.cachedValue == null)
                {
                    value.load(value.defaultValue);
                }

                map.put(key, value.cachedValue);
            }

            writer.print(gson.toJson(map));
        }

        checkFileWatcher();

        logDebugFileOperation("Saved");
    }

    private void _read() throws Exception
    {
        logDebugFileOperation("Reading");

        Gson gson = new Gson();
        String json = Files.readString(this.filePair.right().toPath());

        Object2ObjectOpenHashMap<String, Object> map = gson.fromJson(json, new TypeToken<Object2ObjectOpenHashMap<String, Object>>()
        {
        }.getType());

        if (map != null && map.size() > 0)
        {
            for (String key : this.config.keySet())
            {
                var storedValueObject = this.config.get(key);
                var loadedValue = map.get(key);

                if (loadedValue == null)
                {
                    this.state = State.NEEDS_SAVING;
                    loadedValue = storedValueObject.defaultValue;
                }

                storedValueObject.load(loadedValue);
            }
        }

        if (this.state != State.READY)
        {
            if (this.state == State.UNINITIALIZED)
            {
                if (this.fileWatcher == null)
                {
                    this.fileWatcherExecutor.shutdown();
                }
                else
                {
                    this.fileWatcherExecutor.submit(this.fileWatcher::watch);
                }
            }

            if (this.state == State.NEEDS_SAVING)
            {
                this.log.warn("Config " + this.fileName + " has incorrect/missing values - fixing");

                save();
            }

            this.state = State.READY;
        }

        checkFileWatcher();

        logDebugFileOperation("Finished reading");
    }

    private void checkFileWatcher()
    {
        if (this.fileWatcher != null && !this.fileWatcher.isAlive())
        {
            this.fileWatcherExecutor.shutdown();
        }
    }

    private void logDebugFileOperation(String message)
    {
        this.log.info(message + " " + this.fileName);
    }

    private void fail(String message, Throwable e)
    {
        this.log.error(message + " " + this.fileName, e);
    }

    private void shutdownExecutor(ExecutorService service)
    {
        boolean terminated;

        try
        {
            terminated = service.awaitTermination(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            terminated = false;
        }

        if (!terminated)
        {
            service.shutdownNow();
        }
    }

    private static ExecutorService makeIoExecutor()
    {
        return Executors.newSingleThreadExecutor((runnable) ->
        {
            Thread thread = new Thread(runnable);
            thread.setName(JsonConfig.class.getSimpleName() + "-Worker-" + WORKER_COUNT.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    private enum State
    {
        UNINITIALIZED,
        READY,
        NEEDS_SAVING
    }

    public class Value<V> implements Supplier<V>
    {
        private final String key;
        protected final V defaultValue;

        private V cachedValue = null;

        private Value(String key, V defaultValue)
        {
            this.key = key;
            this.defaultValue = defaultValue;

            if (JsonConfig.this.config.containsKey(key))
            {
                throw new IllegalArgumentException("Can't have duplicate config entries! " + key);
            }

            JsonConfig.this.config.putIfAbsent(key, this);
        }

        @Override
        public V get()
        {
            check();

            return this.cachedValue;
        }

        @SuppressWarnings("unchecked")
        public Class<V> getValueClass()
        {
            return (Class<V>) this.defaultValue.getClass();
        }

        public V getDefaultValue()
        {
            return this.defaultValue;
        }

        public String getName()
        {
            return this.key;
        }

        public void set(V value)
        {
            check();

            this.cachedValue = value;
        }

        public void reset()
        {
            check();

            this.cachedValue = this.defaultValue;
        }

        public void save()
        {
            check();

            JsonConfig.this.save();
        }

        @SuppressWarnings("unchecked")
        protected void load(Object value)
        {
            Class<?> clazz = this.defaultValue.getClass();

            if (clazz.equals(value.getClass()))
            {
                this.cachedValue = (V) value;
            }
            else
            {
                JsonConfig.this.log.warn("Found incorrect value in file {} for key {}, \"{}\". Loading default: {} ({})", JsonConfig.this.fileName, this.key, value, this.defaultValue, clazz.getSimpleName());

                this.cachedValue = this.defaultValue;

                JsonConfig.this.state = State.NEEDS_SAVING;
            }
        }

        private void check()
        {
            if (!JsonConfig.this.isReady() || this.cachedValue == null)
            {
                throw new IllegalStateException("Can't access config values before config is loaded! " + JsonConfig.this.fileName);
            }
        }
    }

    public abstract class NumberValue<N extends Number> extends Value<N>
    {
        protected final N min;
        protected final N max;

        private NumberValue(String key, N defaultValue, N min, N max)
        {
            super(key, defaultValue);

            this.min = min;
            this.max = max;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void load(Object value)
        {
            if (value instanceof Double convertible)
            {
                Class<N> targetType = (Class<N>) this.getDefaultValue().getClass();
                N converted;

                if (targetType == Double.class)
                {
                    converted = targetType.cast(convertible);
                }
                else if (targetType == Float.class)
                {
                    converted = targetType.cast(convertible.floatValue());
                }
                else if (targetType == Long.class)
                {
                    converted = targetType.cast(convertible.longValue());
                }
                else if (targetType == Integer.class)
                {
                    converted = targetType.cast(convertible.intValue());
                }
                else if (targetType == Short.class)
                {
                    converted = targetType.cast(convertible.shortValue());
                }
                else if (targetType == Byte.class)
                {
                    converted = targetType.cast(convertible.byteValue());
                }
                else
                {
                    throw new IllegalArgumentException("Unsupported number type: " + targetType);
                }

                if (isValueOutsideRange(converted))
                {
                    JsonConfig.this.log.warn(getOutOfRangeMessage(converted, this) + ", using default: " + this.defaultValue);

                    converted = this.defaultValue;

                    JsonConfig.this.state = State.NEEDS_SAVING;
                }

                value = converted;
            }

            super.load(value);
        }

        public boolean isValueOutsideRange(N value)
        {
            double test = value.doubleValue();

            return test < this.min.doubleValue() || test > this.max.doubleValue();
        }

        public N getMin()
        {
            return this.min;
        }

        public N getMax()
        {
            return this.max;
        }
    }

    public class BooleanValue extends Value<Boolean>
    {
        private BooleanValue(String key, Boolean defaultValue)
        {
            super(key, defaultValue);
        }
    }

    public class IntValue extends NumberValue<Integer>
    {
        private IntValue(String key, int defaultValue, int min, int max)
        {
            super(key, defaultValue, min, max);
        }
    }

    public class LongValue extends NumberValue<Long>
    {
        private LongValue(String key, long defaultValue, long min, long max)
        {
            super(key, defaultValue, min, max);
        }
    }

    public class DoubleValue extends NumberValue<Double>
    {
        private DoubleValue(String key, double defaultValue, double min, double max)
        {
            super(key, defaultValue, min, max);
        }
    }

    public class FloatValue extends NumberValue<Float>
    {
        private FloatValue(String key, float defaultValue, float min, float max)
        {
            super(key, defaultValue, min, max);
        }
    }
}
