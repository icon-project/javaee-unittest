/*
 * Copyright 2020 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package score.impl.struct;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class MethodCollector extends ClassVisitor {
    private String superName;
    private final List<MemberDecl> decls = new ArrayList<>();

    public MethodCollector() {
        super(Opcodes.ASM7);
    }

    @Override
    public void visit(int version, int access, String name,
            String signature, String superName, String[] interfaces) {
        this.superName = superName;
        super.visit(version, access, name, signature, superName,
                interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name,
            String descriptor, String signature, String[] exceptions) {
        decls.add(new MemberDecl(access, name, descriptor));
        return super.visitMethod(access, name, descriptor, signature,
                exceptions);
    }

    public String getSuperName() {
        return superName;
    }

    public List<MemberDecl> getMethodDecls() {
        return decls;
    }
}
