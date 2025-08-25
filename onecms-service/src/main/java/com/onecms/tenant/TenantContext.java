package com.onecms.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Thread-local tenant context for multi-tenant data isolation
 */
@Slf4j
public class TenantContext {
    
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";
    
    /**
     * Set the current tenant for the thread
     */
    public static void setCurrentTenant(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            currentTenant.set(tenantId.toLowerCase().trim());
            log.debug("Set tenant context: {}", tenantId);
        } else {
            log.warn("Attempted to set null or empty tenant ID, using default");
            currentTenant.set(DEFAULT_TENANT);
        }
    }
    
    /**
     * Get the current tenant for the thread
     */
    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        if (tenant == null) {
            log.debug("No tenant context set, using default tenant");
            return DEFAULT_TENANT;
        }
        return tenant;
    }
    
    /**
     * Check if a specific tenant is currently active
     */
    public static boolean isCurrentTenant(String tenantId) {
        return getCurrentTenant().equals(tenantId);
    }
    
    /**
     * Clear the tenant context for the current thread
     */
    public static void clear() {
        String previousTenant = currentTenant.get();
        currentTenant.remove();
        if (previousTenant != null) {
            log.debug("Cleared tenant context: {}", previousTenant);
        }
    }
    
    /**
     * Execute a block of code with a specific tenant context
     */
    public static <T> T withTenant(String tenantId, TenantCallable<T> callable) throws Exception {
        String previousTenant = getCurrentTenant();
        try {
            setCurrentTenant(tenantId);
            return callable.call();
        } finally {
            if (previousTenant != null) {
                setCurrentTenant(previousTenant);
            } else {
                clear();
            }
        }
    }
    
    /**
     * Execute a block of code with a specific tenant context (void return)
     */
    public static void withTenant(String tenantId, TenantRunnable runnable) throws Exception {
        withTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * Get the default tenant ID
     */
    public static String getDefaultTenant() {
        return DEFAULT_TENANT;
    }
    
    /**
     * Check if current tenant is the default tenant
     */
    public static boolean isDefaultTenant() {
        return DEFAULT_TENANT.equals(getCurrentTenant());
    }
    
    @FunctionalInterface
    public interface TenantCallable<T> {
        T call() throws Exception;
    }
    
    @FunctionalInterface
    public interface TenantRunnable {
        void run() throws Exception;
    }
}