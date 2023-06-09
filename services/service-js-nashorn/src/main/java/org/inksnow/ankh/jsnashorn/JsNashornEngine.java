package org.inksnow.ankh.jsnashorn;

import lombok.SneakyThrows;
import org.inksnow.ankh.core.api.script.AnkhScriptEngine;
import org.inksnow.ankh.core.api.script.PreparedScript;
import org.inksnow.ankh.core.api.script.ScriptContext;
import org.inksnow.ankh.core.script.ScriptCacheStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class JsNashornEngine implements AnkhScriptEngine {
  private static final Logger logger = LoggerFactory.getLogger("ankh-js-nashorn");
  private static final MethodHandle nashornFactory = createFactory();

  @SneakyThrows
  private static ScriptEngineFactory getFactory() {
    try {
      return (ScriptEngineFactory) Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory")
          .getConstructor()
          .newInstance();
    } catch (UnsupportedClassVersionError e) {
      return new ScriptEngineManager()
          .getEngineFactories()
          .stream()
          .filter(it -> it.getNames().contains("nashorn"))
          .findFirst()
          .orElse(null);
    }
  }

  private static <T extends Throwable> MethodHandle createFactory() throws T {
    ScriptEngineFactory factory = getFactory();
    if (factory == null) {
      throw new IllegalStateException("No support js engine found");
    }
    Class<? extends ScriptEngineFactory> clazz = factory.getClass();
    try {
      return MethodHandles.lookup().findVirtual(
          clazz,
          "getScriptEngine",
          MethodType.methodType(ScriptEngine.class, String[].class, ClassLoader.class)
      ).bindTo(factory).asType(
          MethodType.methodType(Compilable.class, String[].class, ClassLoader.class)
      );
    } catch (Exception e) {
      throw (T) e;
    }
  }


  @Override
  public PreparedScript prepare(String script) throws Exception {
    return new NashornPps(script);
  }

  private static class NashornPps implements PreparedScript {
    private final String script;
    private final ScriptCacheStack<CompiledScript, ScriptException> cacheStack;

    public NashornPps(String script) throws ScriptException {
      this.script = script;
      this.cacheStack = new ScriptCacheStack<>(this::createScript);
      this.cacheStack.prepare(1);
    }

    private CompiledScript createScript() throws ScriptException {
      return createEngine().compile(script);
    }

    private <T extends Throwable> Compilable createEngine() throws T {
      try {
        return (Compilable) nashornFactory.invokeExact(
            new String[]{"--global-per-engine"},
            this.getClass().getClassLoader()
        );
      } catch (Throwable e) {
        throw (T) e;
      }
    }

    @Override
    public Object execute(ScriptContext context) throws Exception {
      CompiledScript compile = cacheStack.borrow();
      try {
        return compile.eval(new JavaxScriptContext(context));
      } finally {
        cacheStack.sendBack(compile);
      }
    }
  }
}
