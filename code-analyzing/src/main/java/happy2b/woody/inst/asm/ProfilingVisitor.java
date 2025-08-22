package happy2b.woody.inst.asm;

import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.utils.Pair;
import happy2b.woody.inst.advice.ProfilingAdvice;
import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.MethodIntrospection;
import happy2b.woody.inst.tools.ProfilingEntity;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;


public class ProfilingVisitor extends MethodVisitor {

    private static final Logger log = LoggerFactory.getLogger(ProfilingVisitor.class);
    private static final String INTROSPECTING_ADVICE = ProfilingAdvice.class.getName().replace(".", "/");
    private static final String PROFILING_ENTITY = "L" + ProfilingEntity.class.getName().replace(".", "/") + ";";

    private int startLine;
    private int endLine;
    private int lineNumber;
    private Method method;
    private int[][] visitFlags;
    private int localVariableIndex;
    private Map<Integer, int[]> handlerLines = new HashMap<>();

    private Label startLabel;
    private Label endLabel = new Label();

    public ProfilingVisitor(int api, MethodVisitor mv, Method method) {
        super(api, mv);
        this.method = method;
        MethodIntrospection introspection = Config.get().getMethodLineIntrospection(method);
        this.startLine = introspection.getStartLine();
        this.endLine = introspection.getEndLine();
        int[][] visitFlags = introspection.getVisitFlags();
        this.visitFlags = new int[visitFlags.length][];
        this.localVariableIndex = method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
        for (int i = 0; i < visitFlags.length; i++) {
            if (visitFlags[i] != null) {
                this.visitFlags[i] = new int[visitFlags[i].length];
                System.arraycopy(visitFlags[i], 0, this.visitFlags[i], 0, visitFlags[i].length);
            }
        }
    }


    @Override
    public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;

        if (isCandidateLine() && shouldStartProfiling(line)) {
            mv.visitLdcInsn(lineNumber);
            mv.visitVarInsn(ALOAD, localVariableIndex);
            mv.visitMethodInsn(INVOKESTATIC, INTROSPECTING_ADVICE, ProfilingAdvice.START_PROFILING.getName(), MethodUtil.getMethodDescriptor(ProfilingAdvice.START_PROFILING), false);
            log.info("Insert before executor for  class:{}, method:{},  line: {}", method.getDeclaringClass().getName(), method.getName(), lineNumber);
            handlerLines.get(lineNumber)[0] = 1;
        }

        super.visitLineNumber(line, start);

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
    public void visitVarInsn(int opcode, int varIndex) {
        if (varIndex >= localVariableIndex) {
            varIndex += 1;
        }

        super.visitVarInsn(opcode, varIndex);

        int index = lineNumber - startLine;

        if (isCandidateLine() && --visitFlags[index][0] == 0 && shouldFinishProfiling(lineNumber)) {
            mv.visitLdcInsn(lineNumber);
            mv.visitVarInsn(ALOAD, localVariableIndex);
            mv.visitMethodInsn(INVOKESTATIC, INTROSPECTING_ADVICE, ProfilingAdvice.FINISH_PROFILING.getName(), MethodUtil.getMethodDescriptor(ProfilingAdvice.FINISH_PROFILING), false);
            log.info("Insert after executor for class:{}, method:{},  line: {}", method.getDeclaringClass().getName(), method.getName(), lineNumber);
            if (visitFlags[index].length == 2) {
                visitFlags[index][0] = visitFlags[index][1];
                visitFlags[index][1] = 0;
                handlerLines.remove(lineNumber);
            } else {
                handlerLines.get(lineNumber)[1] = 1;
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        int index = lineNumber - startLine;

        if (isCandidateLine() && --visitFlags[index][0] == 0 && shouldFinishProfiling(lineNumber)) {
            mv.visitLdcInsn(lineNumber);
            mv.visitVarInsn(ALOAD, localVariableIndex);
            mv.visitMethodInsn(INVOKESTATIC, INTROSPECTING_ADVICE, ProfilingAdvice.FINISH_PROFILING.getName(), MethodUtil.getMethodDescriptor(ProfilingAdvice.FINISH_PROFILING), false);
            if (visitFlags[index].length == 2) {
                visitFlags[index][0] = visitFlags[index][1];
                visitFlags[index][1] = 0;
                handlerLines.remove(lineNumber);
            } else {
                handlerLines.get(lineNumber)[1] = 1;
            }
            log.info("Insert after executor for class:{}, method:{},  line: {}", method.getDeclaringClass().getName(), method.getName(), lineNumber);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (isCandidateLine()) {
            visitFlags[lineNumber - startLine][0]--;
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    private boolean isCandidateLine() {
        return lineNumber >= startLine && lineNumber <= endLine;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        if (!Modifier.isStatic(method.getModifiers())) {
            startLabel = new Label();
            visitLabel(startLabel);
            visitVarInsn(ALOAD, 0);
            visitLdcInsn(method.getName());

            int paramLength = method.getParameterTypes().length;

            Pair<Boolean, Integer> code = findOperationCode(paramLength);
            if (code.getLeft()) {
                visitInsn(code.getRight());
            } else {
                visitIntInsn(BIPUSH, code.getRight());
            }
            visitTypeInsn(ANEWARRAY, "java/lang/Object");
            visitMethodParams();

            visitMethodInsn(INVOKESTATIC, INTROSPECTING_ADVICE, ProfilingAdvice.ON_METHOD_ENTER.getName(), MethodUtil.getMethodDescriptor(ProfilingAdvice.ON_METHOD_ENTER), false);
            mv.visitVarInsn(ASTORE, localVariableIndex);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("dfPE", PROFILING_ENTITY, null, startLabel, endLabel, localVariableIndex);
        super.visitMaxs(maxStack + 2, maxLocals + 1);
    }

    @Override
    public void visitInsn(int opcode) {
        // 在所有返回指令前插入代码
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            mv.visitVarInsn(ALOAD, localVariableIndex);
            mv.visitMethodInsn(INVOKESTATIC, INTROSPECTING_ADVICE, ProfilingAdvice.ON_METHOD_EXIT.getName(), MethodUtil.getMethodDescriptor(ProfilingAdvice.ON_METHOD_EXIT), false);
            log.info("Insert exit executor for class:{}, method:{},  line: {}", method.getDeclaringClass().getName(), method.getName(), lineNumber);
        }
        super.visitInsn(opcode);
    }

    private void visitMethodParams() {
        int length = method.getParameterTypes().length;
        for (int i = 0; i < length; i++) {
            mv.visitInsn(DUP);

            Pair<Boolean, Integer> code = findOperationCode(i);
            if (code.getLeft()) {
                mv.visitInsn(code.getRight());
            } else {
                mv.visitIntInsn(BIPUSH, code.getRight());
            }
            mv.visitVarInsn(ALOAD, i + 1);
            mv.visitInsn(AASTORE);
        }
    }

    private Pair<Boolean, Integer> findOperationCode(int x) {
        switch (x) {
            case 0:
                return Pair.of(true, ICONST_0);
            case 1:
                return Pair.of(true, ICONST_1);
            case 2:
                return Pair.of(true, ICONST_2);
            case 3:
                return Pair.of(true, ICONST_3);
            case 4:
                return Pair.of(true, ICONST_4);
            case 5:
                return Pair.of(true, ICONST_5);
            default:
                return Pair.of(false, x);
        }
    }

    private boolean shouldStartProfiling(int line) {
        int[] ints = handlerLines.computeIfAbsent(line, integer -> new int[2]);
        if (ints[0] > 0) {
            return false;
        }
        MethodIntrospection intro = Config.get().getMethodLineIntrospection(method);
        return intro.isOnCodeBlockEnter(line) || intro.isInsnLine(line);
    }

    private boolean shouldFinishProfiling(int line) {
        int[] ints = handlerLines.computeIfAbsent(line, integer -> new int[2]);
        if (ints[1] > 0) {
            return false;
        }
        MethodIntrospection intro = Config.get().getMethodLineIntrospection(method);
        return intro.isOnCodeBlockExit(line) || intro.isInsnLine(line);
    }

}
