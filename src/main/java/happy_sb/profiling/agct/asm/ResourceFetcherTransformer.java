package happy_sb.profiling.agct.asm;

import happy_sb.profiling.agct.tool.ignore.IgnoredTypesBuilder;
import happy_sb.profiling.agct.tool.ignore.IgnoredTypesBuilderImpl;
import happy_sb.profiling.agct.tool.ignore.IgnoredTypesPredicate;
import happy_sb.profiling.utils.MethodUtil;
import net.bytebuddy.jar.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static happy_sb.profiling.agct.asm.ResourceMethodFetcherAdvice.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/22
 */
public class ResourceFetcherTransformer implements ClassFileTransformer {


    private IgnoredTypesPredicate typesPredicate;

    public ResourceFetcherTransformer() {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        builder.allowClass(SPRING_WEB_INSTRUMENTATION_CLASS);
        builder.allowClass(DUBBO_INSTRUMENTATION_CLASS);
        typesPredicate = builder.buildTransformIgnoredPredicate();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!typesPredicate.test(loader, className)) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        if (className.equals(SPRING_WEB_INSTRUMENTATION_CLASS)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (SPRING_WEB_INSTRUMENTATION_METHOD.equals(name)) {
                                      return new SpringWebResourceFetcherVisitor(Opcodes.ASM7, mv);
                                  }
                                  return mv;
                              }
                          },
                    0);
            return writer.toByteArray();
        } else if (className.equals(DUBBO_INSTRUMENTATION_CLASS)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (DUBBO_INSTRUMENTATION_METHOD.equals(name)) {
                                      return new DubboResourceFetcherVisitor(Opcodes.ASM7, mv);
                                  }
                                  return mv;
                              }
                          },
                    0);
            return writer.toByteArray();
        } else {
            return null;
        }
    }

    private class SpringWebResourceFetcherVisitor extends MethodVisitor {

        private boolean visited = false;

        protected SpringWebResourceFetcherVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, SPRING_WEB_FETCHER_METHOD.getName(), MethodUtil.getMethodDescriptor(SPRING_WEB_FETCHER_METHOD), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

    private class DubboResourceFetcherVisitor extends MethodVisitor {

        private boolean visited = false;

        protected DubboResourceFetcherVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, DUBBO_INSTRUMENTATION_CLASS, "getInterfaceClass", "()Ljava/lang/Class;", false);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, DUBBO_INSTRUMENTATION_CLASS, "getRef", "()Ljava/lang/Object;", false);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, DUBBO_FETCHER_METHOD.getName(), MethodUtil.getMethodDescriptor(DUBBO_FETCHER_METHOD), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

}
