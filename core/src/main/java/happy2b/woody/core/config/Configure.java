package happy2b.woody.core.config;

import happy2b.woody.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

/**
 * <pre>
 * 配置类。
 * 注意本类里的所有字段不能有默认值，否则会出现配置混乱。
 * 在 com.taobao.arthas.core.Arthas#attach 里会调用 Configure#toStrig
 * <pre>
 *
 * @author vlinux
 * @author hengyunabc 2018-11-12
 */
public class Configure {

    private String ip;
    private Integer serverPort;
    private Integer javaPid;
    private String woodyCore;
    private String woodyAgent;


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }


    public Integer getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(Integer javaPid) {
        this.javaPid = javaPid;
    }

    public String getWoodyAgent() {
        return woodyAgent;
    }

    public void setWoodyAgent(String woodyAgent) {
        this.woodyAgent = woodyAgent;
    }

    public String getWoodyCore() {
        return woodyCore;
    }

    public void setWoodyCore(String woodyCore) {
        this.woodyCore = woodyCore;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 序列化成字符串
     *
     * @return 序列化字符串
     */
    @Override
    public String toString() {

        final Map<String, String> map = new HashMap<String, String>();
        for (Field field : Configure.class.getDeclaredFields()) {
            // 过滤掉静态类
            if (isStatic(field.getModifiers())) {
                continue;
            }

            // 非静态的才需要纳入非序列化过程
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(this);
                if (fieldValue != null) {
                    map.put(field.getName(), String.valueOf(fieldValue));
                }
            } catch (Throwable t) {
                //
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
        }
        return sb.toString().trim();
    }


    /**
     * 反序列化字符串成对象
     *
     * @param toString 序列化字符串
     * @return 反序列化的对象
     */
    public static Configure toConfigure(String toString) {
        System.out.println(toString);
        final Configure configure = new Configure();
        String[] split = toString.split(" ");
        for (int i = 0; i < split.length; i++) {
            String[] kv = split[i].split("=");
            String fieldName = kv[0];
            String fieldValue = kv[1];
            System.out.println(fieldName + "=" + fieldValue);
            Field field = ReflectionUtils.findField(Configure.class, fieldName);
            Object value = fieldValue;
            if (field.getType() == Long.class) {
                value = Long.valueOf(fieldValue);
            } else if (field.getType() == Integer.class) {
                value = Integer.valueOf(fieldValue);
            }
            ReflectionUtils.set(configure, fieldName, value);
        }
        return configure;
    }


}
