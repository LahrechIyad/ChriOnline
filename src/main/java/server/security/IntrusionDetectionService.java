package server.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IntrusionDetectionService {
    private static final long WINDOW_MS = 60_000;
    private static final long BLOCK_MS = 5 * 60_000;
    private static final int MAX_AUTH_FAILURES = 3;
    private static final int MAX_OTP_FAILURES = 3;
    private static final int MAX_REQUESTS_PER_WINDOW = 120;

    private static final Map<String, Deque<Long>> failedLoginsByKey = new ConcurrentHashMap<>();
    private static final Map<String, Deque<Long>> failedOtpByKey = new ConcurrentHashMap<>();
    private static final Map<String, Deque<Long>> requestsByIp = new ConcurrentHashMap<>();
    private static final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    private IntrusionDetectionService() {
    }

    public static boolean isBlocked(String ipAddress) {
        Long blockedAt = blockedIps.get(ipAddress);
        if (blockedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - blockedAt > BLOCK_MS) {
            blockedIps.remove(ipAddress);
            return false;
        }
        return true;
    }

    public static void recordRequest(String ipAddress) {
        if (ipAddress == null) {
            return;
        }
        int count = addAndCount(requestsByIp, ipAddress);
        if (count > MAX_REQUESTS_PER_WINDOW) {
            blockIp(ipAddress, "-", "REQUEST_FLOOD", count + " requests in less than 1 minute");
        }
    }

    public static void recordLoginFailure(String email, String ipAddress) {
        String key = key(email, ipAddress);
        int count = addAndCount(failedLoginsByKey, key);
        if (count >= MAX_AUTH_FAILURES) {
            SecurityEventLogger.alert(email, ipAddress, "BRUTE_FORCE_LOGIN", count + " failed login attempts");
        }
    }

    public static void recordOtpFailure(String email, String ipAddress) {
        String key = key(email, ipAddress);
        int count = addAndCount(failedOtpByKey, key);
        if (count >= MAX_OTP_FAILURES) {
            SecurityEventLogger.alert(email, ipAddress, "OTP_ABUSE", count + " invalid OTP attempts");
        }
    }

    public static void recordAdminDenied(String username, String ipAddress, String action) {
        SecurityEventLogger.alert(username, ipAddress, "ADMIN_ACCESS_DENIED", action);
    }

    public static void blockIp(String ipAddress, String username, String action, String reason) {
        if (ipAddress == null) {
            return;
        }
        blockedIps.put(ipAddress, System.currentTimeMillis());
        SecurityEventLogger.alert(username, ipAddress, action, "IP blocked for 5 minutes: " + reason);
    }

    private static int addAndCount(Map<String, Deque<Long>> events, String key) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            timestamps.addLast(now);
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.removeFirst();
            }
            return timestamps.size();
        }
    }

    private static String key(String username, String ipAddress) {
        return (username == null ? "-" : username.toLowerCase()) + "@" + (ipAddress == null ? "-" : ipAddress);
    }
}
