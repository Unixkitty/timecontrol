package com.unixkitty.timecontrol.config.json;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;

public class ConfigFileWatcher
{
    private final Path path;
    private final Runnable configReloader;

    private boolean keepWorking = true;
    private boolean scheduledReload = false;
    private WatchService watchService = null;

    public ConfigFileWatcher(JsonConfig config) throws IOException
    {
        this.path = config.getFile().toPath().toAbsolutePath().normalize();
        this.configReloader = config::load;
    }

    public void watch()
    {
        final Path name = this.path.getFileName();
        final Logger log = LogManager.getLogger(ConfigFileWatcher.class);

        try
        {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.path.getParent().register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            log.info("Watching {} for filesystem changes", name.toString());

            while (this.keepWorking && !Thread.currentThread().isInterrupted())
            {
                WatchKey key = this.watchService.take();

                for (WatchEvent<?> event : key.pollEvents())
                {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && event.context() instanceof Path changedPath && changedPath.equals(name) && canScheduleReload())
                    {
                        log.info("{} changed on disk, reloading", name.toString());
                        this.configReloader.run();

                        setScheduledReload(true);
                    }
                }

                key.reset();
            }
        }
        catch (InterruptedException | IOException e)
        {
            if (e instanceof InterruptedException)
            {
                log.info("{} file watcher interrupted, stopping", name.toString());
            }
            else
            {
                log.error("Caught " + ((IOException) e).getClass().getSimpleName() + " watching " + name.toString(), e);
            }

            stop();
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void stop()
    {
        this.keepWorking = false;

        if (this.watchService != null)
        {
            try
            {
                this.watchService.close();
            }
            catch (IOException ignored)
            {

            }
        }
    }

    public synchronized boolean isAlive()
    {
        return this.keepWorking;
    }

    public synchronized void setScheduledReload(boolean state)
    {
        this.scheduledReload = state;
    }

    private synchronized boolean canScheduleReload()
    {
        return !this.scheduledReload;
    }
}
