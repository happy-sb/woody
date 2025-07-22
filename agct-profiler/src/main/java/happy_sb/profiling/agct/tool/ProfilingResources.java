package happy_sb.profiling.agct.tool;

import happy_sb.profiling.agct.core.AGCTProfilerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProfilingResources {

  private static final Logger log = LoggerFactory.getLogger(ProfilingResources.class);

  private Map<String, String> httpResources;
  private Set<String> dubboResourceTypes;
  private Map<String, String> dubboResources;
  private Set<String> grpcResourceTypes;
  private Map<String, String> grpcResources;
  private Set<String> rocketmqResourceTypes;
  private Map<String, String> rocketmqResources;
  private Set<String> kafkaResourceTypes;
  private Map<String, String> kafkaResources;

  private Set<Method> rabbitResourceMethods;


  public ProfilingResources() {
    this.httpResources = new ConcurrentHashMap<>(1024);
    this.dubboResourceTypes = ConcurrentHashMap.newKeySet(16);
    this.dubboResources = new ConcurrentHashMap<>(16);
    this.grpcResourceTypes = ConcurrentHashMap.newKeySet(16);
    this.grpcResources = new ConcurrentHashMap<>(16);
    this.rocketmqResourceTypes = ConcurrentHashMap.newKeySet(16);
    this.rocketmqResources = new ConcurrentHashMap<>(16);
    this.kafkaResourceTypes = ConcurrentHashMap.newKeySet(16);
    this.kafkaResources = new ConcurrentHashMap<>(16);
    this.rabbitResourceMethods = ConcurrentHashMap.newKeySet(16);
  }

  public void addHttpResources(String path, String resource, Method method) {
    if (httpResources.size() > 1024) {
      httpResources.clear();
    }
    httpResources.computeIfAbsent(path, new Function<String, String>() {
      @Override
      public String apply(String s) {
        AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("http", method));
        return resource;
      }
    });
  }

  public void addGrpcResources(String fullName, Class type) {
    if (grpcResourceTypes.contains(type.getName())) {
      return;
    }
    grpcResourceTypes.add(type.getName());
    for (Method method : type.getDeclaredMethods()) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 2 && parameterTypes[1].getName().equals("io.grpc.stub.StreamObserver")) {
        grpcResources.put(fullName, type.getName() + "." + method.getName());
        AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("grpc", method));
      }
    }
  }

  public void addApacheDubboResources(Class type, String implement) {
    addDubboResources(type, implement);
  }

  public void addAlibabaDubboResources(Class type, String implement) {
    addDubboResources(type, implement);
  }

  private void addDubboResources(Class type, String implement) {
    if (dubboResourceTypes.contains(type.getName()) || implement.startsWith("org.apache.dubbo.") || implement.startsWith("com.alibaba.dubbo.")) {
      return;
    }
    try {
      Class<?> implementClass = type.getClassLoader().loadClass(implement);
      dubboResourceTypes.add(type.getName());
      for (Method method : type.getDeclaredMethods()) {
        dubboResources.put(getMethodDesc(method), implement + "." + method.getName());
        Method m = implementClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
        AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("dubbo", m));
      }
    } catch (Exception e) {
      log.error("Load dubbo class failed!", e);
    }
  }


  public void addRocketMQResource(String topic, Class type, String method) {
    String typeName = type.getName();
    if (typeName.startsWith("org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer")) {
      return;
    }
    if (rocketmqResourceTypes.contains(type.getName())) {
      return;
    }
    rocketmqResources.put("Consume Topic " + topic, type.getName() + "." + method);
    AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("rocketmq", findMostApplicableMethod(type, method)));
  }

  public void addKafkaResource(String topic, Method method) {
    kafkaResourceTypes.add(method.getDeclaringClass().getName());
    kafkaResources.put("Consume Topic " + topic, method.getDeclaringClass().getName() + "." + method.getName());
    AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("kafka", method));
  }

  public void addRabbitResource(Method method) {
    if (rabbitResourceMethods.contains(method)) {
      return;
    }
    AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod("rabbitmq", method));
  }

  private String getMethodDesc(Method method) {
    String className = method.getDeclaringClass().getName();
    Class<?>[] types = method.getParameterTypes();
    StringBuilder sb = new StringBuilder(className).append(".").append(method.getName()).append("(");
    for (int i = 0; i < types.length; i++) {
      sb.append(types[i].getSimpleName());
      if (i < types.length - 1) {
        sb.append(",");
      }
    }
    sb.append(")");
    return sb.toString();
  }

  public Map<String, String> getInverseProfilingResources() {
    Map<String, String> resources = new HashMap<>();
    putInverseMap(httpResources, resources);
    putInverseMap(dubboResources, resources);
    putInverseMap(grpcResources, resources);
    putInverseMap(rocketmqResources, resources);
    putInverseMap(kafkaResources, resources);
    return resources;
  }

  private void putInverseMap(Map<String, String> source, Map<String, String> target) {
    for (Map.Entry<String, String> entry : source.entrySet()) {
      target.put(entry.getValue(), entry.getKey());
    }
  }

  public String getResourceEntrance(String resource) {
    String entrance = httpResources.get(resource);
    if (entrance == null) {
      entrance = dubboResources.get(resource);
    }
    if (entrance == null) {
      entrance = grpcResources.get(resource);
    }
    if (entrance == null) {
      entrance = rocketmqResources.get(resource);
    }
    if (entrance == null) {
      entrance = kafkaResources.get(resource);
    }
    return entrance;
  }

  private Method findMostApplicableMethod(Class type, String method) {
    List<Method> namedMethods = new ArrayList<>();
    for (Method declaredMethod : type.getDeclaredMethods()) {
      if (!declaredMethod.getName().equals(method)) {
        continue;
      }
      namedMethods.add(declaredMethod);
    }
    if (namedMethods.isEmpty()) {
      return null;
    }
    if (namedMethods.size() == 1) {
      return namedMethods.get(0);
    }

    namedMethods = namedMethods.stream().filter(new Predicate<Method>() {
      @Override
      public boolean test(Method method) {
        return !(method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class);
      }
    }).collect(Collectors.toList());

    return namedMethods.stream().sorted(new Comparator<Method>() {
      @Override
      public int compare(Method o1, Method o2) {
        // 优先取public方法
        boolean pub1 = Modifier.isPublic(o1.getModifiers());
        boolean pub2 = Modifier.isPublic(o2.getModifiers());
        if (pub1 && !pub2) {
          return -1;
        }
        if (!pub1 && pub2) {
          return 1;
        }
        // 优先取参数长的方法
        int l1 = o1.getParameterCount();
        int l2 = o2.getParameterCount();
        return l1 > l2 ? -1 : 1;
      }
    }).findFirst().get();

  }

  public void clearHttpResources() {
    httpResources.clear();
  }
}
