package foundation.icon.ee.io;

import java.math.BigInteger;

public class RLPNDataReader extends AbstractRLPDataReader implements DataReader {
    public RLPNDataReader(byte[] data) {
        super(data);
    }

    @Override
    protected int readNull(byte[] ba, int offset, int len) {
        if (len < 2) {
            return 0;
        }
        if (ba[offset] == (byte) 0xf8 && (ba[offset + 1] == 0)) {
            return 2;
        }
        return 0;
    }

    @Override
    protected BigInteger readBigInteger(byte[] ba, int offset, int len) {
        return new BigInteger(ba, offset, len);
    }
}
