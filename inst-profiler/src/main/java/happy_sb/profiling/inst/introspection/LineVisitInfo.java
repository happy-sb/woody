package happy_sb.profiling.inst.introspection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class LineVisitInfo {
  private int line;
  private String owner;
  private String name;
  private String desc;
  private Field field;
  private Method targetMethod;

  public LineVisitInfo(int line, String owner, String name, String desc, Field field) {
    this.line = line;
    this.owner = owner;
    this.name = name;
    this.field = field;
    if (this.field != null) {
      this.field.setAccessible(true);
    }
    this.desc = desc;
  }

  public int getLine() {
    return line;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Method getTargetMethod() {
    return targetMethod;
  }

  public void setTargetMethod(Method targetMethod) {
    this.targetMethod = targetMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LineVisitInfo that = (LineVisitInfo) o;
    return line == that.line && Objects.equals(owner, that.owner) && Objects.equals(name, that.name) && Objects.equals(desc, that.desc) && Objects.equals(field, that.field) && Objects.equals(targetMethod, that.targetMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, owner, name, desc, field, targetMethod);
  }
}
