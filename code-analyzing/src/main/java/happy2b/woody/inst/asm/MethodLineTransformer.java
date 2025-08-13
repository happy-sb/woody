package happy2b.woody.inst.asm;

import happy2b.woody.util.MethodUtil;
import happy2b.woody.util.bytecode.SourceCodeExtractor;
import happy2b.woody.util.reflection.ReflectionUtils;
import happy2b.woody.api.Config;
import net.bytebuddy.jar.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;

public class MethodLineTransformer implements ClassFileTransformer {

  private Class targetClass;
  private Collection<Method> methods;
  private List<String> excludes;
  private boolean eagerRefresh = false;

  public MethodLineTransformer(Class targetClass, Collection<Method> methods, List<String> excludes) {
    this.targetClass = targetClass;
    this.methods = methods;
    this.excludes = excludes;
    String className = targetClass.getName();
    if (Character.isDigit(className.charAt(className.length() - 1))) {
      eagerRefresh = true;
    }
    for (int i = 0; i < excludes.size(); i++) {
      String exclude = excludes.get(i);
      if (exclude.endsWith(".**")) {
        exclude = exclude.substring(0, exclude.length() - 3);
      }
      excludes.set(i, exclude.replace(".", "/"));
    }
    Config.get().addLineIntrospectClass(targetClass);
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    if (targetClass != classBeingRedefined) {
      return classfileBuffer;
    }
    SourceCodeExtractor.saveSourceCode(this.getClass(), className, classfileBuffer);

    ClassReader reader = new ClassReader(classfileBuffer);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
    reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                      MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                      Method method = findMethod(access, name, descriptor);
                      return method == null ? mv : new MethodLineVisitor(Opcodes.ASM7, mv, method, excludes, eagerRefresh);
                    }
                  },
        0);
    return writer.toByteArray();
  }

  public Method findMethod(int access, String name, String descriptor) {
    for (Method method : ReflectionUtils.getDeclaredMethods(targetClass)) {
      int modifiers = method.getModifiers();
      String methodName = method.getName();
      String methodDescriptor = MethodUtil.getMethodDescriptor(method);
      if (modifiers == access && methodName.equals(name) && descriptor.equals(methodDescriptor)) {
        if (methods.contains(method) && !Config.get().hasMethodIntrospected(targetClass, method)) {
          return method;
        } else {
          return null;
        }
      }
    }
    return null;
  }


}
