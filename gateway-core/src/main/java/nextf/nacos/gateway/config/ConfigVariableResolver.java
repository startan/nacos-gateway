package nextf.nacos.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration variable resolver
 * Supports syntax:
 * - ${env:VAR_NAME} - Environment variables
 * - ${sys:property.name} - System properties
 * - ${VAR_NAME} - Try environment variable first, then system property
 * - ${VAR_NAME:-default} - With default value
 */
public class ConfigVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(ConfigVariableResolver.class);

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final int MAX_RECURSION_DEPTH = 10;

    /**
     * Resolve all variables in configuration content
     * @param content Original configuration content
     * @return Configuration content with variables replaced
     */
    public static String resolve(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;
        int depth = 0;

        // Support recursive resolution (variables containing variables)
        while (depth < MAX_RECURSION_DEPTH) {
            Matcher matcher = VAR_PATTERN.matcher(result);
            if (!matcher.find()) {
                break; // No more variables to replace
            }

            matcher.reset();
            StringBuffer sb = new StringBuffer();
            boolean hasReplacement = false;

            while (matcher.find()) {
                String varExpression = matcher.group(1); // Remove ${}
                String value = resolveVariable(varExpression);

                if (value != null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                    hasReplacement = true;
                    log.debug("Resolved variable: ${{{}}} -> {}", varExpression, value);
                } else {
                    // Variable not found, keep original and log warning
                    log.warn("Variable not found: ${{{}}}, keeping original", varExpression);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(sb);

            result = sb.toString();
            if (!hasReplacement) {
                break; // No replacement in this round, exit
            }
            depth++;
        }

        if (depth >= MAX_RECURSION_DEPTH) {
            log.warn("Reached maximum recursion depth ({}) while resolving variables", MAX_RECURSION_DEPTH);
        }

        return result;
    }

    /**
     * Resolve a single variable, supports default value syntax ${VAR:-default}
     * @param varExpression Variable expression (may contain prefix like env: or sys:)
     * @return Resolved value, or null if not found
     */
    private static String resolveVariable(String varExpression) {
        String varName;
        String defaultValue = null;

        // Parse default value separator ":-"
        // Note: Need to correctly handle nested ${} structures
        int depth = 0;
        int defaultSeparator = -1;
        for (int i = 0; i < varExpression.length() - 1; i++) {
            char c = varExpression.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (depth == 0 && c == ':' && varExpression.charAt(i + 1) == '-') {
                defaultSeparator = i;
                break;
            }
        }

        if (defaultSeparator > 0) {
            varName = varExpression.substring(0, defaultSeparator);
            defaultValue = varExpression.substring(defaultSeparator + 2);
        } else {
            varName = varExpression;
        }

        String value = null;

        if (varName.startsWith("env:")) {
            // Environment variable: ${env:VAR_NAME}
            String envVarName = varName.substring(4);
            value = System.getenv(envVarName);
        } else if (varName.startsWith("sys:")) {
            // System property: ${sys:property.name}
            String propName = varName.substring(4);
            value = System.getProperty(propName);
        } else {
            // Shorthand syntax: try environment variable first, then system property
            value = System.getenv(varName);
            if (value == null) {
                value = System.getProperty(varName);
            }
        }

        // Return value if found, otherwise return default value (if exists)
        if (value != null) {
            return value;
        }
        return defaultValue;
    }
}
