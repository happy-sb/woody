package happy2b.profiling.agct.asm;

import happy2b.profiler.util.MethodUtil;
import happy2b.profiling.agct.tool.AGCTPredicate;
import happy2b.profiling.agct.resource.ResourceMethod;
import happy2b.profiling.agct.resource.ResourceMethodManager;
import net.bytebuddy.jar.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import static happy2b.profiling.agct.asm.TracingMethodAdvice.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class TracingMethodTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
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
                              if (includeMethod != null) {
                                  return new TracingMethodVisitor(Opcodes.ASM7, mv, includeMethod);
                              }
                              return mv;
                          }
                      },
                0);
        return writer.toByteArray();

    }

    private class TracingMethodVisitor extends MethodVisitor {

        private ResourceMethod includeMethod;

        private int localVariableIndex;

        private Label startTraceLabel = new Label();
        private Label methodEnd = new Label();

        private Label tryStartLabel = new Label();
        private Label tryEndLabel = new Label();
        private Label catchLabel = new Label();

        private int returnOpcode = -1;

        protected TracingMethodVisitor(int api, MethodVisitor methodVisitor, ResourceMethod includeMethod) {
            super(api, methodVisitor);
            this.includeMethod = includeMethod;
            Method method = includeMethod.getMethod();
            this.localVariableIndex = method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
        }

        @Override
        public void visitCode() {
            super.visitCode();

            mv.visitLabel(startTraceLabel);
            mv.visitLdcInsn(includeMethod.getResourceType());
            mv.visitLdcInsn(includeMethod.getResource());
            mv.visitLdcInsn(includeMethod.getMethodPath());
            mv.visitLdcInsn(includeMethod.getIdGenerator().getOrder());
            mv.visitMethodInsn(INVOKESTATIC, ADVICE_CLASS, START_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(START_TRACE_METHOD), false);
            mv.visitVarInsn(ASTORE, localVariableIndex);

            mv.visitLabel(tryStartLabel);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (index >= localVariableIndex) {
                index += 1;
            }
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitIincInsn(int varIndex, int increment) {
            if (varIndex >= localVariableIndex) {
                varIndex += 1;
            }
            super.visitIincInsn(varIndex, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
                returnOpcode = opcode;
                mv.visitLabel(tryEndLabel);
                mv.visitJumpInsn(Opcodes.GOTO, catchLabel);
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (varIndex >= localVariableIndex) {
                varIndex += 1;
            }
            super.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {

            mv.visitLabel(catchLabel);

            mv.visitVarInsn(Opcodes.ALOAD, localVariableIndex);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PROFILING_TRACE_CLASS, FINISH_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(FINISH_TRACE_METHOD), false);

            mv.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchLabel, null);

            if (returnOpcode == Opcodes.ATHROW) {
                mv.visitInsn(Opcodes.ATHROW);
            } else if (returnOpcode != -1) {
                mv.visitInsn(returnOpcode);
            }
            mv.visitLabel(methodEnd);

            mv.visitLocalVariable("oneTrace", PROFILING_TRACE_CLASS, null, startTraceLabel, methodEnd, localVariableIndex);

            super.visitMaxs(Math.max(maxStack, 5), Math.max(maxLocals, 2));
        }
    }
}
