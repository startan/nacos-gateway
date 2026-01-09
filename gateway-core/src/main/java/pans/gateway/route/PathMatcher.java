package pans.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Path matcher with wildcard support (* and **)
 */
public class PathMatcher {

    private static final Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private final String pattern;
    private final Pattern regex;
    private final boolean isDoubleStar;
    private final boolean isSingleStar;
    private final String prefix;

    public PathMatcher(String pattern) {
        this.pattern = pattern;
        this.isDoubleStar = pattern.endsWith("/**");
        this.isSingleStar = pattern.endsWith("/*") && !isDoubleStar;

        if (isDoubleStar) {
            this.prefix = pattern.substring(0, pattern.length() - 3);
            this.regex = null;
        } else if (isSingleStar) {
            this.prefix = pattern.substring(0, pattern.length() - 2);
            this.regex = null;
        } else if (pattern.contains("*")) {
            this.prefix = null;
            this.regex = compilePattern(pattern);
        } else {
            this.prefix = null;
            this.regex = null;
        }
    }

    private Pattern compilePattern(String pattern) {
        // Convert wildcard to regex
        // /api/* -> /api/[^/]+
        // /api/** -> /api/.*
        // /api/*/test -> /api/[^/]+/test
        String regex = pattern.replace(".", "\\.")
                .replace("**", ".*");

        // Replace single * (not yet replaced by **)
        // Need to be careful with the order
        regex = regex.replaceAll("\\*(?!\\*)", "[^/]*");

        return Pattern.compile("^" + regex + "$");
    }

    public boolean matches(String path) {
        if (path == null) {
            return false;
        }

        // Fast path: exact match
        if (!isDoubleStar && !isSingleStar && regex == null) {
            return pattern.equals(path);
        }

        // /abc/** matches /abc and all sub-paths
        if (isDoubleStar) {
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }

        // /abc/* matches /abc/ with single level only
        if (isSingleStar) {
            if (!path.startsWith(prefix + "/")) {
                return false;
            }
            String remaining = path.substring(prefix.length() + 1);
            return remaining.indexOf('/') == -1;
        }

        // Complex pattern: use regex
        return regex.matcher(path).matches();
    }

    public String getPattern() {
        return pattern;
    }
}
