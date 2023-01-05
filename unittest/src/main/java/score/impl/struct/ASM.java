package score.impl.struct;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class ASM {
    public static<T extends ClassVisitor> T accept(byte[] classBytes, T cv) {
        var cr = new ClassReader(classBytes);
        cr.accept(cv, 0);
        return cv;
    }
}
