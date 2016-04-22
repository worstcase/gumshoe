package com.dell.gumshoe.file;

import java.util.regex.Pattern;

/** match file using wildcard patterns
 *
 * wildcards: * = zero or more chars in a file or dir name, ? = exactly one char, ** = one or more directory levels
 */
public class PathPatternMatcher implements FileMatcher {
    private static final boolean IS_WINDOWS = "\\".equals(System.getProperty("file.separator"));
    private static final Pattern INVALID1 = Pattern.compile(IS_WINDOWS ? ".*\\*\\*[^\\\\].*" : ".*\\*\\*[^/].*");
    private static final Pattern INVALID2 = Pattern.compile(IS_WINDOWS ? ".*[^\\\\]\\*\\*.*" : ".*[^/]\\*\\*.*");
    private final Pattern pattern;

    public PathPatternMatcher(String wildcardPattern) {
        // the "**" cannot be used inside of a name, can only have / to its left or right
        if(INVALID1.matcher(wildcardPattern).matches() || INVALID2.matcher(wildcardPattern).matches()) {
            throw new IllegalArgumentException("invalid wildcard pattern: " + wildcardPattern);
        }

        // keep logic consistent -- use / for pattern.  if this was windows, reverse them to unix style temporarily
        // NOTE: this means windows can support / or \ in the pattern, so same config files will work for both
        if(IS_WINDOWS) {
            wildcardPattern = wildcardPattern.replaceAll("\\\\", "/");
        }

        final String regex =
                wildcardPattern.replaceAll("\\.", "\\\\.")      // "." is literal in filename
                               .replaceAll("\\*\\*/", ".+/")    // "**/" matches any string followed by /
                               .replaceAll("/\\*\\*", "/.+")    // "/**" matches / followed by any string
                               .replaceAll("\\*", "[^/]*")      // "*" matches >=0 file chars (but not /)
                               .replaceAll("\\?", "[^/]");      // "?" matches one file char (not /)

        // if original was simple filename (no dir info) then assume any dir is ok
        final String pathRegex = wildcardPattern.contains("/") ? regex : ("(.*/)?"+regex);

        // on windows, reverse all the slashes
        final String osRegex = IS_WINDOWS ? pathRegex.replaceAll("/", "\\\\") : pathRegex;

        this.pattern = Pattern.compile(osRegex);
    }

    @Override
    public boolean matches(String path) {
        return pattern.matcher(path).matches();
    }
}
