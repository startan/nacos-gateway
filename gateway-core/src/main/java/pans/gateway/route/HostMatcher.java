package pans.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Host matcher with wildcard support (*.example.com)
 */
public class HostMatcher {

    private static final Logger log = LoggerFactory.getLogger(HostMatcher.class);

    private final String pattern;
    private Pattern regex;

    public HostMatcher(String pattern) {
        this.pattern = pattern;
        this.regex = compilePattern(pattern);
    }

    private Pattern compilePattern(String pattern) {
        // Fast path: exact match
        if (!pattern.contains("*")) {
            return null;
        }

        // Convert wildcard to regex
        // *.example.com -> [^.]+\.example\.com
        String regex = pattern.replace(".", "\\.")
                .replace("*", "[^.]+");

        return Pattern.compile("^" + regex + "$");
    }

    public boolean matches(String host) {
        if (host == null) {
            return false;
        }

        // Exact match
        if (regex == null) {
            return pattern.equalsIgnoreCase(host);
        }

        // Wildcard match
        return regex.matcher(host).matches();
    }
}
