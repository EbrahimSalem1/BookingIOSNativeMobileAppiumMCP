package com.booking.automation.driver;

import com.booking.automation.config.ConfigurationException;
import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands out devices to parallel test threads so that two sessions never share a simulator
 * or a WebDriverAgent port. Pool size therefore caps useful parallelism:
 * {@code -Dthreads=N} should be &le; number of entries in {@code device.pool}.
 */
public final class DevicePool {

    private static final Logger LOG = LoggerFactory.getLogger(DevicePool.class);
    private static final Duration LEASE_TIMEOUT = Duration.ofMinutes(10);
    private static volatile DevicePool instance;

    private final BlockingQueue<DeviceSpec> available;

    DevicePool(List<DeviceSpec> devices) {
        if (devices.isEmpty()) {
            throw new ConfigurationException("'" + ConfigKeys.DEVICE_POOL + "' must list at least one device");
        }
        this.available = new LinkedBlockingQueue<>(devices);
        LOG.info("Device pool initialised with {} device(s): {}", devices.size(), devices);
    }

    public static DevicePool get() {
        if (instance == null) {
            synchronized (DevicePool.class) {
                if (instance == null) {
                    instance = fromConfig(FrameworkConfig.get());
                }
            }
        }
        return instance;
    }

    static DevicePool fromConfig(FrameworkConfig config) {
        String version = config.getString(ConfigKeys.DEVICE_PLATFORM_VERSION);
        int basePort = config.getInt(ConfigKeys.DEVICE_WDA_BASE_PORT, 8100);
        AtomicInteger index = new AtomicInteger();
        List<DeviceSpec> devices = Arrays.stream(config.getString(ConfigKeys.DEVICE_POOL).split(","))
                .filter(entry -> !entry.isBlank())
                .map(entry -> DeviceSpec.parse(entry, version, basePort + index.getAndIncrement()))
                .toList();
        return new DevicePool(devices);
    }

    public DeviceSpec lease() {
        try {
            DeviceSpec device = available.poll(LEASE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (device == null) {
                throw new IllegalStateException("No free device after " + LEASE_TIMEOUT
                        + ". Reduce -Dthreads or add devices to '" + ConfigKeys.DEVICE_POOL + "'.");
            }
            LOG.debug("Thread '{}' leased {}", Thread.currentThread().getName(), device);
            return device;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a device", e);
        }
    }

    public void release(DeviceSpec device) {
        if (device != null && available.offer(device)) {
            LOG.debug("Thread '{}' released {}", Thread.currentThread().getName(), device);
        }
    }
}
