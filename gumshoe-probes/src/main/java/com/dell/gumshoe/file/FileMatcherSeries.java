package com.dell.gumshoe.file;


/** select file combining lists of accept and reject matchers */
public class FileMatcherSeries implements FileMatcher {
    final FileMatcher[] acceptList;
    final FileMatcher[] rejectList;

    public FileMatcherSeries(FileMatcher[] acceptList, FileMatcher[] rejectList) {
        this.acceptList = acceptList;
        this.rejectList = rejectList;
    }

    /** a path matches this series only if:
     *  - it is not null
     *  - it does not match any in the reject list
     *  - it matches at least one in the accept list
     *
     *  note this behavior is slightly different than the SocketMatcherSeries.match.
     *  here reject overrides accept; for sockets accept will override reject.
     */
    @Override
    public boolean matches(String path) {
        if(path==null) { return false; }

        for(FileMatcher reject : rejectList) {
            if(reject.matches(path)) {
                return false;
            }
        }

        if(acceptList.length==0) { return true; }
        for(FileMatcher accept : acceptList) {
            if(accept.matches(path)) {
                return true;
            }
        }

        return false;
    }
}
