package su.nightexpress.excellenteconomy.util;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import su.nightexpress.nightcore.bridge.scheduler.AdaptedScheduler;
import su.nightexpress.nightcore.bridge.scheduler.AdaptedTask;

import java.util.concurrent.TimeUnit;

public class FoliaScheduler implements AdaptedScheduler {

    private final JavaPlugin plugin;
    private final RegionScheduler regionScheduler;
    private final GlobalRegionScheduler globalRegionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaScheduler(@NonNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionScheduler = plugin.getServer().getRegionScheduler();
        this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
        this.asyncScheduler = plugin.getServer().getAsyncScheduler();
    }

    @Override
    public void cancelTasks() {
        this.globalRegionScheduler.cancelTasks(this.plugin);
        this.asyncScheduler.cancelTasks(this.plugin);
    }

    @Override
    @NonNull
    public AdaptedTask runTask(@NonNull Runnable runnable) {
        return new Task(this.globalRegionScheduler.run(this.plugin, task -> runnable.run()));
    }

    @Override
    @Nullable
    public AdaptedTask runTask(@NonNull Entity entity, @NonNull Runnable runnable) {
        ScheduledTask task = entity.getScheduler().run(this.plugin, scheduledTask -> runnable.run(), null);
        return task == null ? null : new Task(task);
    }

    @Override
    @NonNull
    public AdaptedTask runTask(@NonNull Location location, @NonNull Runnable runnable) {
        return new Task(this.regionScheduler.run(this.plugin, location, task -> runnable.run()));
    }

    @Override
    @NonNull
    public AdaptedTask runTask(@NonNull Chunk chunk, @NonNull Runnable runnable) {
        return new Task(this.regionScheduler.run(this.plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(),
            task -> runnable.run()));
    }

    @Override
    @NonNull
    public AdaptedTask runTaskAsync(@NonNull Runnable runnable) {
        return new Task(this.asyncScheduler.runNow(this.plugin, task -> runnable.run()));
    }

    @Override
    @NonNull
    public AdaptedTask runTaskLater(@NonNull Runnable runnable, long delay) {
        if (delay <= 0L) {
            return this.runTask(runnable);
        }
        return new Task(this.globalRegionScheduler.runDelayed(this.plugin, task -> runnable.run(), delay));
    }

    @Override
    @NonNull
    public AdaptedTask runTaskLaterAsync(@NonNull Runnable runnable, long delay) {
        if (delay <= 0L) {
            return this.runTaskAsync(runnable);
        }
        return new Task(this.asyncScheduler.runDelayed(this.plugin, task -> runnable.run(), delay * 50L,
            TimeUnit.MILLISECONDS));
    }

    @Override
    @NonNull
    public AdaptedTask runTaskTimer(@NonNull Runnable runnable, long delay, long period) {
        return new Task(this.globalRegionScheduler.runAtFixedRate(this.plugin, task -> runnable.run(),
            Math.max(1L, delay), period));
    }

    @Override
    @NonNull
    public AdaptedTask runTaskTimerAsync(@NonNull Runnable runnable, long delay, long period) {
        return new Task(this.asyncScheduler.runAtFixedRate(this.plugin, task -> runnable.run(),
            Math.max(1L, delay) * 50L, period * 50L, TimeUnit.MILLISECONDS));
    }

    private static final class Task implements AdaptedTask {

        private final ScheduledTask backend;

        private Task(@NonNull ScheduledTask backend) {
            this.backend = backend;
        }

        @Override
        public void cancel() {
            this.backend.cancel();
        }

        @Override
        public boolean isCancelled() {
            return this.backend.isCancelled();
        }

        @Override
        @NonNull
        public Plugin getOwningPlugin() {
            return this.backend.getOwningPlugin();
        }

        @Override
        public boolean isCurrentlyRunning() {
            ScheduledTask.ExecutionState state = this.backend.getExecutionState();
            return state == ScheduledTask.ExecutionState.RUNNING
                || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
        }

        @Override
        public boolean isRepeatingTask() {
            return this.backend.isRepeatingTask();
        }
    }
}
