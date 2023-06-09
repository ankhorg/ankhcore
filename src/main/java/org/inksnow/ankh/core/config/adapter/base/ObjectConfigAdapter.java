package org.inksnow.ankh.core.config.adapter.base;

import com.google.gson.reflect.TypeToken;
import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.inksnow.ankh.core.api.config.ConfigLoader;
import org.inksnow.ankh.core.api.config.ConfigSection;
import org.inksnow.ankh.core.api.config.ConfigTypeAdapter;
import org.inksnow.ankh.core.api.config.exception.ConfigException;
import org.inksnow.ankh.core.api.config.exception.ConfigValidateException;
import org.inksnow.ankh.core.common.util.BootstrapUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("rawtypes")
public class ObjectConfigAdapter<T> implements ConfigTypeAdapter<T> {
  private final Class clazz;
  private final TypedEntry[] typedEntries;

  @SuppressWarnings("unchecked")
  @SneakyThrows
  @Override
  public T read(ConfigSection section) {
    val instance = (T) BootstrapUtil.unsafe().allocateInstance(clazz);
    val exceptions = new LinkedList<ConfigException.Entry>();
    for (val typedEntry : typedEntries) {
      if (typedEntry.adapter != null) {
        val subSection = section.get(typedEntry.configName);
        val value = typedEntry.adapter.read(subSection);
        Set<ConstraintViolation> validateResult = ConfigVaildatorUtils.validator().validateValue(clazz, typedEntry.beanName, value);
        for (val violation : validateResult) {
          exceptions.add(new ConfigException.Entry(subSection.source(), violation.getMessage()));
        }
        typedEntry.setter.invoke(instance, value);
      }
    }
    if (exceptions.isEmpty()) {
      return instance;
    } else {
      throw new ConfigValidateException(exceptions);
    }
  }

  public static class Factory implements ConfigTypeAdapter.Factory<Object> {
    @Override
    @SneakyThrows
    public <V> ConfigTypeAdapter<V> create(ConfigLoader configLoader, TypeToken<? super V> typeToken) {
      val rawType = ((TypeToken) typeToken).getRawType();
      if ((rawType.getModifiers() & Modifier.ABSTRACT) != 0 || rawType.isPrimitive() || rawType.isInterface()) {
        return null;
      }
      val typedEntries = Stream.concat(Stream.of(rawType), Arrays.stream(rawType.getInterfaces()))
          .filter(it -> it != Object.class)
          .flatMap(it -> Arrays.stream(it.getDeclaredFields()))
          .filter(it -> !Modifier.isStatic(it.getModifiers()))
          .map(new Function<Field, TypedEntry>() {
            @Override
            @SneakyThrows
            public TypedEntry apply(Field field) {
              val beanName = field.getName();
              val configName = configLoader.translateName(beanName);
              val fieldType = field.getType();
              val fieldTypeToken = TypeToken.get(field.getGenericType());
              val adapter = configLoader.getAdapter(fieldTypeToken);
              val setter = BootstrapUtil.lookup()
                  .findSetter(field.getDeclaringClass(), beanName, fieldType)
                  .asType(MethodType.methodType(void.class, rawType, field.getType()));
              return new TypedEntry(field, beanName, configName, setter, fieldTypeToken, adapter);
            }
          })
          .toArray(TypedEntry[]::new);
      return new ObjectConfigAdapter<>(rawType, typedEntries);
    }
  }

  @RequiredArgsConstructor
  private static class TypedEntry {
    private final Field field;
    private final String beanName;
    private final String configName;
    private final MethodHandle setter;
    private final TypeToken<?> typeToken;
    private final ConfigTypeAdapter<?> adapter;
  }
}
