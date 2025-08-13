package happy2b.woody.inst.asm;

import happy2b.woody.util.MethodUtil;
import happy2b.woody.util.bytecode.SourceCodeExtractor;
import happy2b.woody.util.reflection.ReflectionUtils;
import happy2b.woody.api.Config;
import happy2b.woody.api.MethodIntrospection;
import happy2b.woody.api.LineVisitInfo;
import happy2b.woody.inst.introspection.MethodLineOriginalIntrospection;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class MethodLineVisitor extends MethodVisitor {

    private static final Logger log = LoggerFactory.getLogger(MethodLineVisitor.class);

    private int lineNumber;
    private Method method;
    private List<String> excludes;
    private String classFile;
    private Stack<Field> fields = new Stack<>();
    private boolean eagerRefresh;

    public MethodLineVisitor(int api, MethodVisitor methodVisitor, Method method, List<String> excludes, boolean eagerRefresh) {
        super(api, methodVisitor);
        this.method = method;
        this.excludes = excludes;
        this.classFile = method.getDeclaringClass().getName().replace(".", "/");
        this.eagerRefresh = eagerRefresh;
        Config.get().addMethodLineIntrospection(method, new MethodLineOriginalIntrospection(method, SourceCodeExtractor.extractSourceCode(method)));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        Config.get().getMethodLineIntrospection(method).visit(lineNumber);
        if (!classFile.equals(owner)) {
            super.visitFieldInsn(opcode, owner, name, descriptor);
            return;
        }
        Field field = ReflectionUtils.findField(method.getDeclaringClass(), name);
        if (field != null && (!field.getType().isPrimitive() && field.getType() != String.class)) {
            this.fields.push(field);
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;
        Config.get().getMethodLineIntrospection(method).refreshMethodIntrospectionLine(this.lineNumber);
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        Config.get().getMethodLineIntrospection(method).addContentLine(this.lineNumber, new HashSet<>(), false);
        super.visitVarInsn(opcode, varIndex);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        Field field = matchField(owner);
        MethodIntrospection lineIntrospection = Config.get().getMethodLineIntrospection(method);
        if (!excluded(owner) && (field != null || owner.equals(classFile))) {
            lineIntrospection.addInstLine(this.lineNumber, buildMatchKeywords(field, owner, name, descriptor));
        }
        lineIntrospection.addContentLine(this.lineNumber, buildMatchKeywords(field, owner, name, descriptor), !shouldSkip(owner, name));

        if (!excluded(owner)) {
            lineIntrospection.getVisitInfos().computeIfAbsent(lineNumber, integer -> new HashSet<>())
                    .add(new LineVisitInfo(lineNumber, owner, name, descriptor, (field != null || owner.equals(classFile)) ? field : null));
        }

        if (isNewAnonymousClassInstance(owner, name)) {
            lineIntrospection.addAnonymousClass(lineNumber, owner);
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        if (eagerRefresh) {
            Config.get().getMethodLineIntrospection(method).refreshIntrospectLines();
        }
    }

    private Field matchField(String owner) {
        if (!fields.isEmpty()) {
            String clazz = fields.peek().getType().getName().replace(".", "/");
            if (clazz.equals(owner)) {
                return fields.pop();
            }
        }
        return null;
    }

    private boolean excluded(String owner) {
        for (String exclude : excludes) {
            if (owner.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> buildMatchKeywords(Field field, String owner, String name, String desc) {
        Set<String> kws = new HashSet<>();
        if (shouldSkip(owner, name)) {
            return kws;
        }

        if ("<init>".equals(name)) {
            kws.add("new ");
            String className = extractSimpleClassName(owner);
            if (className != null) {
                kws.add(className);
            }
            return kws;
        }
        if (classFile.equals(owner)) {
            if (isStaticMethod(method.getDeclaringClass(), name, desc)) {
                String className = extractSimpleClassName(owner);
                kws.add(className + "." + name + "(");
            } else {
                kws.add("this." + name + "(");
            }
            return kws;
        }
        if (field != null) {
            if (Modifier.isStatic(field.getModifiers())) {
                kws.add(field.getName() + "." + name + "(");
            } else {
                kws.add("this." + field.getName() + "." + name + "(");
            }
            return kws;
        }
        if (isStaticMethod(owner, name, desc)) {
            kws.add(extractSimpleClassName(owner));
        }
        kws.add("." + name + "(");
        return kws;
    }

    private String extractSimpleClassName(String owner) {
        if (owner == null || !owner.contains("/")) {
            return null;
        }
        if (Character.isDigit(owner.charAt(owner.length() - 1))) {
            return null;
        }
        String clazz = owner.substring(owner.lastIndexOf("/") + 1);
        return clazz.contains("$") ? clazz.substring(clazz.indexOf("$") + 1) : clazz;
    }

    private boolean isStaticMethod(Class clazz, String name, String desc) {
        List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(clazz, name);
        for (Method m : methods) {
            if (MethodUtil.getMethodDescriptor(m).equals(desc)) {
                return Modifier.isStatic(m.getModifiers());
            }
        }
        return false;
    }

    private boolean isStaticMethod(String owner, String name, String desc) {
        try {
            Class clazz = method.getDeclaringClass().getClassLoader().loadClass(owner.replace("/", "."));
            return isStaticMethod(clazz, name, desc);
        } catch (Exception e) {
            log.error("databuff-profiling: Load class {} failed!", owner);
        }
        return false;
    }

    private boolean shouldSkip(String owner, String name) {
        if ("java/lang/Integer".equals(owner) && "intValue".equals(name)) {
            return true;
        }
        if ("java/lang/Double".equals(owner) && "doubleValue".equals(name)) {
            return true;
        }
        if ("java/lang/Long".equals(owner) && "longValue".equals(name)) {
            return true;
        }
        if ("java/lang/Float".equals(owner) && "floatValue".equals(name)) {
            return true;
        }
        if ("getClass".equals(name) || ("java/lang/Boolean".equals(owner) && "booleanValue".equals(name))) {
            return true;
        }
        return false;
    }

    private boolean isNewAnonymousClassInstance(String owner, String methodName) {
        if (!owner.contains("$")) {
            return false;
        }
        String classNo = owner.substring(owner.lastIndexOf("$") + 1);
        try {
            int id = Integer.valueOf(classNo);
            return id > 0 && methodName.equals("<init>");
        } catch (Exception e) {
            return false;
        }
    }

}
