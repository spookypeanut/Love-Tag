package uk.co.spookypeanut.lovetag;

public class Tag {
    String mName;
    boolean mActive;
    boolean mPresent;
    public Tag(String name) {
        mName = name;
        mActive = false;
        mPresent = false;
    }
    public String toString() {
        return mName;
    }
}
