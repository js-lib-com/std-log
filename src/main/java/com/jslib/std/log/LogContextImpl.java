package com.jslib.std.log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jslib.api.log.LogContext;

class LogContextImpl implements LogContext
{
  private static final InheritableThreadLocal<LogContextImpl> threadContext = new InheritableThreadLocal<>();

  public static LogContextImpl get()
  {
    LogContextImpl context = threadContext.get();
    if(context == null) {
      synchronized(threadContext) {
        context = threadContext.get();
        if(context == null) {
          context = new LogContextImpl();
          threadContext.set(context);
        }
      }
    }
    return context;
  }

  private final Map<String, String> values;

  public LogContextImpl()
  {
    this.values = new LinkedHashMap<>();
  }

  @Override
  public void put(String name, String value)
  {
    values.put(name, value);
  }

  @Override
  public String get(String name)
  {
    return values.get(name);
  }

  public void forEach(BiConsumer<String, String> consumer)
  {
    values.forEach(consumer);
  }

  @Override
  public void clear()
  {
    values.clear();
  }
}
