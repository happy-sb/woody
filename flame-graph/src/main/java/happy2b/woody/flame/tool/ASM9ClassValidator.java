package happy2b.woody.flame.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

public class ASM9ClassValidator {

    public static String validateClass(String classFilePath) {
        File classFile = new File(classFilePath);
        if (!classFile.exists()) {
            return "文件不存在: " + classFilePath;
        }

        byte[] classBytes;
        try {
            classBytes = readClassFile(classFile);
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }

        // 捕获验证错误信息
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(baos);

        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor checkAdapter = new CheckClassAdapter(cw, true);
            cr.accept(checkAdapter, ClassReader.EXPAND_FRAMES);
            String errors = baos.toString().trim();
            return errors.isEmpty() ? "Class文件格式正确" : "验证错误:\n" + errors;
        } catch (Exception e) {
            return "验证异常: " + e.getMessage();
        } finally {
            errorStream.close();
        }
    }

    private static byte[] readClassFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = fis.read(buffer);
            if (bytesRead != file.length()) {
                throw new IOException("文件不完整，预期 " + file.length() + " 字节，实际读取 " + bytesRead + " 字节");
            }
            return buffer;
        }
    }

    public static void main(String[] args) {
        // 示例：验证一个 class 文件
        String classPath = "classpath";
        String result = validateClass(classPath);

        if (result.isEmpty()) {
            System.out.println("Class 文件格式正确");
        } else {
            System.err.println(result);
        }
    }
}


