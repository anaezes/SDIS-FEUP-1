package Common.messages;

import java.io.Serializable;

/*
 * This is the version of the protocol. It is a three ASCII char sequence with the format <n>'.'<m>,
 * where <n> and <m> are the ASCII codes of digits. For example, version 1.0, the one specified in this document,
 * should be encoded as the char sequence '1''.''0'.
 */
public class Version implements Serializable {
    private int n, m;
    private boolean unset = true;

    public Version() { }

    public Version(int n, int m) {
        unset = false;
        this.n = n;
        this.m = m;
    }

    public void setVersion(Version v) {
        unset = v.unset;
        n = v.n;
        m = v.m;
    }

    public void setVersion(int n, int m) {
        unset = false;
        this.n = n;
        this.m = m;
    }

    @Override
    public String toString() {
        return unset ? "" : this.n + "." + this.m;
    }
}
