package com.dell.gumshoe.file;

import junit.framework.TestCase;

public class TestPathMatcher extends TestCase {
    private void assertMatches(String path, String pattern) {
        assertTrue(new PathPatternMatcher(pattern).matches(path));
    }

    private void assertNoMatch(String path, String pattern) {
        assertFalse(new PathPatternMatcher(pattern).matches(path));
    }

    private void assertInvalid(String pattern) {
        try {
            new PathPatternMatcher(pattern);
            fail("pattern should be invalid");
        } catch(IllegalArgumentException e) {
            return;
        }
    }

    public void testFilenamePattern() {
        assertMatches("filename.txt", "*.txt");
        assertNoMatch("filename.doc", "*.txt");
        assertMatches("filename.txt", "f*n*t");
        assertNoMatch("filename.doc", "f*n*t");
        assertMatches("filename.txt", "*.*");
        assertNoMatch("core", "*.*");

        assertMatches("relative/filename.txt", "*.txt");
        assertNoMatch("relative/filename.doc", "*.txt");
        assertMatches("relative/filename.txt", "f*n*t");
        assertNoMatch("relative/filename.doc", "f*n*t");
        assertMatches("relative/filename.txt", "*.*");
        assertNoMatch("relative/core", "*.*");

        assertMatches("/absolute/filename.txt", "*.txt");
        assertNoMatch("/absolute/filename.doc", "*.txt");
        assertMatches("/absolute/filename.txt", "f*n*t");
        assertNoMatch("/absolute/filename.doc", "f*n*t");
        assertMatches("/absolute/filename.txt", "*.*");
        assertNoMatch("/absolute/core", "*.*");
    }

    public void testDirnamePattern() {
        assertMatches("some/dirname/file.txt", "some/dirname/*");
        assertMatches("some/dirname/file.txt", "*/dirname/file.txt");
        assertMatches("some/dirname/file.txt", "*/dirname/*");
        assertMatches("some/dirname/file.txt", "**/dirname/**");

        assertNoMatch("/absolute/some/dirname/file.txt", "some/dirname/*");
        assertNoMatch("/absolute/some/dirname/file.txt", "*/dirname/file.txt");
        assertNoMatch("/absolute/some/dirname/file.txt", "*/dirname/*");
        assertMatches("/absolute/some/dirname/file.txt", "**/dirname/**");

        assertMatches("relative/a/b/c/d/file.txt", "**/c/**");
        assertMatches("relative/a/b/c/d/file.txt", "**/d/file.txt");
        assertNoMatch("relative/a/b/c/d/file.txt", "**/c/file.txt");
        assertNoMatch("relative/a/b/c/d/file.txt", "relative/**/c/*");
        assertMatches("relative/a/b/c/d/file.txt", "relative/**/c/**");
        assertMatches("relative/a/b/c/d/file.txt", "relative/**/b/**/d/**");
        assertNoMatch("relative/a/b/c/d/file.txt", "relative/**/b/**/c/**");
        assertNoMatch("relative/a/b/c/d/file.txt", "relative/**/d/**/b/**");
    }

    public void testInvalid() {
        assertInvalid("some**thing");
        assertInvalid("some/**thing");
        assertInvalid("some**/thing");
    }
}
