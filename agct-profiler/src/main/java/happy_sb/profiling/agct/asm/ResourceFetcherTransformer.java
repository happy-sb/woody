package happy_sb.profiling.agct.asm;

import happy_sb.profiler.util.MethodUtil;
import happy_sb.profiling.agct.tool.AGCTPredicate;
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


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!AGCTPredicate.acceptResourceFetching(loader, className)) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        if (SPRING_WEB_INSTRUMENTATION_CLASS.equals(className)) {
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
        } else if (DUBBO_INSTRUMENTATION_CLASS.equals(className)) {
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
        } else if (GRPC_INSTRUMENTATION_CLASS.equals(className)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (GRPC_INSTRUMENTATION_METHOD_1.equals(name) || GRPC_INSTRUMENTATION_METHOD_2.equals(name)) {
                                      if (descriptor.startsWith("(Lio/grpc/BindableService;")) {
                                          return new GrpcResourceFetcherVisitor(Opcodes.ASM7, mv);
                                      }
                                  }
                                  return mv;
                              }
                          },
                    0);
            return writer.toByteArray();
        } else if (ROCKETMQ_INSTRUMENTATION_CLASS_1.equals(className)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (ROCKETMQ_INSTRUMENTATION_METHOD_1_1.equals(name) || ROCKETMQ_INSTRUMENTATION_METHOD_1_2.equals(name)) {
                                      return new RocketMQResourceFetcherVisitor_1(Opcodes.ASM7, mv);
                                  }
                                  return mv;
                              }
                          },
                    0);
            return writer.toByteArray();
        } else if (ROCKETMQ_INSTRUMENTATION_CLASS_2.equals(className)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (ROCKETMQ_INSTRUMENTATION_METHOD_2_1.equals(name) || ROCKETMQ_INSTRUMENTATION_METHOD_2_2.equals(name)) {
                                      return new RocketMQResourceFetcherVisitor_2(Opcodes.ASM7, mv);
                                  }
                                  return mv;
                              }
                          },
                    0);
            return writer.toByteArray();
        } else if (KAFKA_INSTRUMENTATION_CLASS.equals(className)) {
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                              @Override
                              public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                  MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                  if (KAFKA_INSTRUMENTATION_METHOD.equals(name)) {
                                      return new KafkaResourceFetcherVisitor(Opcodes.ASM7, mv);
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

    private class GrpcResourceFetcherVisitor extends MethodVisitor {

        private boolean visited = false;

        protected GrpcResourceFetcherVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, GRPC_FETCHER_METHOD.getName(), MethodUtil.getMethodDescriptor(GRPC_FETCHER_METHOD), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

    private class RocketMQResourceFetcherVisitor_1 extends MethodVisitor {

        private boolean visited = false;

        protected RocketMQResourceFetcherVisitor_1(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, ROCKETMQ_FETCHER_METHOD_1.getName(), MethodUtil.getMethodDescriptor(ROCKETMQ_FETCHER_METHOD_1), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

    private class RocketMQResourceFetcherVisitor_2 extends MethodVisitor {

        private boolean visited = false;

        protected RocketMQResourceFetcherVisitor_2(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, ROCKETMQ_FETCHER_METHOD_2.getName(), MethodUtil.getMethodDescriptor(ROCKETMQ_FETCHER_METHOD_2), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

    private class KafkaResourceFetcherVisitor extends MethodVisitor {

        private boolean visited = false;

        protected KafkaResourceFetcherVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!visited) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKESTATIC, FETCHER_ADVICE_CLASS, KAFKA_FETCHER_METHOD.getName(), MethodUtil.getMethodDescriptor(KAFKA_FETCHER_METHOD), false);
                visited = true;
            }
            super.visitLineNumber(line, start);
        }
    }

}
