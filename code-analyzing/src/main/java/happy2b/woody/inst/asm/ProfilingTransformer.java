package happy2b.woody.inst.asm;

import happy2b.woody.util.common.MethodUtil;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Set;

public class ProfilingTransformer implements ClassFileTransformer {

    private Class targetClass;
    private Set<Method> transformedMethods;

    public ProfilingTransformer(Class clazz, Set<Method> methods) {
        this.targetClass = clazz;
        this.transformedMethods = methods;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (targetClass != classBeingRedefined) {
            return classfileBuffer;
        }
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                          @Override
                          public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                              MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                              Method method = filterMethod(access, name, descriptor);
                              return method == null ? mv : new ProfilingVisitor(Opcodes.ASM7, mv, method);
                          }
                      },
                0);
        return writer.toByteArray();
    }

    public Method filterMethod(int access, String name, String descriptor) {
        for (Method method : transformedMethods) {
            int modifiers = method.getModifiers();
            String methodName = method.getName();
            String methodDescriptor = MethodUtil.getMethodDescriptor(method);
            if (modifiers == access && methodName.equals(name) && descriptor.equals(methodDescriptor)) {
                return method;
            }
        }
        return null;
    }

}
