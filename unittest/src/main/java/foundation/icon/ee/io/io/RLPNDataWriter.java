package foundation.icon.ee.io.io;

import java.math.BigInteger;

public class RLPNDataWriter extends AbstractRLPDataWriter implements DataWriter {

    @Override
    protected byte[] toByteArray(BigInteger bi) {
        return bi.toByteArray();
    }

    @Override
    protected void writeNull(ByteArrayBuilder os) {
        os.write(0xf8);
        os.write(0x00);
    }
}
