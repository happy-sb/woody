package happy2b.profiling.agct.resource.transform;

import happy2b.profiler.util.MethodUtil;
import happy2b.profiling.agct.resource.ResourceMethod;
import happy2b.profiling.agct.resource.ResourceMethodManager;
import happy2b.profiling.agct.tool.AGCTPredicate;
import net.bytebuddy.jar.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import static happy2b.profiling.agct.resource.transform.ResourceMethodAdvice.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class ResourceMethodTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined == null) {
            return null;
        }
        if (!AGCTPredicate.acceptTracing(loader, className)) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                ResourceMethod includeMethod = ResourceMethodManager.findProfilingIncludeMethod(classBeingRedefined.getName(), name, descriptor);
                return includeMethod == null ? mv : new TracingMethodVisitor(Opcodes.ASM7, mv, includeMethod);
            }
        }, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();

    }

    public static class TracingMethodVisitor extends MethodVisitor {

        private ResourceMethod includeMethod;

        private Label startLabel, endLabel;
        private Label tryStartLabel, tryEndLabel, catchLabel;

        private int localVarIndex;

        public TracingMethodVisitor(int api, MethodVisitor methodVisitor, ResourceMethod includeMethod) {
            super(api, methodVisitor);
            this.includeMethod = includeMethod;
            Method method = includeMethod.getMethod();
            this.localVarIndex = method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
        }

        @Override
        public void visitCode() {
            startLabel = new Label();
            endLabel = new Label();
            tryStartLabel = new Label();
            tryEndLabel = new Label();
            catchLabel = new Label();

            mv.visitLabel(startLabel);

            mv.visitLdcInsn(includeMethod.getResourceType());
            mv.visitLdcInsn(includeMethod.getResource());
            mv.visitLdcInsn(includeMethod.getMethodPath());
            mv.visitLdcInsn(includeMethod.getIdGenerator().getOrder());
            mv.visitMethodInsn(INVOKESTATIC, ADVICE_CLASS, START_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(START_TRACE_METHOD), false);
            mv.visitVarInsn(ASTORE, localVarIndex);

            mv.visitLabel(tryStartLabel);

            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                mv.visitVarInsn(ALOAD, localVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PROFILING_TRACE_CLASS, FINISH_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(FINISH_TRACE_METHOD), false);
            }
            super.visitInsn(opcode);
        }


        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (index >= localVarIndex) {
                index += 1;
            }
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitIincInsn(int varIndex, int increment) {
            if (varIndex >= localVarIndex) {
                varIndex += 1;
            }
            super.visitIincInsn(varIndex, increment);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (varIndex >= localVarIndex) {
                varIndex += 1;
            }
            super.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitLabel(tryEndLabel);
            mv.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchLabel, "java/lang/Throwable");

            mv.visitLabel(catchLabel);
            int exceptionIndex = localVarIndex + 1;
            mv.visitVarInsn(ASTORE, exceptionIndex);

            mv.visitVarInsn(ALOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PROFILING_TRACE_CLASS, FINISH_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(FINISH_TRACE_METHOD), false);

            mv.visitVarInsn(ALOAD, exceptionIndex);
            mv.visitInsn(ATHROW);

            mv.visitLabel(endLabel);
            mv.visitLocalVariable("oneTrace", PROFILING_TRACE_CLASS, null, startLabel, endLabel, localVarIndex);

            super.visitMaxs(maxStack, maxLocals);
        }
    }
}
