package com.jslib.std.log;

import java.util.HashMap;
import java.util.Map;

public class LogEvent implements Event
{
  private final long timestamp;
  private final String threadName;
  private final StackTraceElement logStackElement;
  private final Map<String, Object> parameters;
  private final String loggerName;
  private final Level level;
  private final String message;
  private final Object[] arguments;

  public LogEvent(String loggerName, Level level, String message, Object[] arguments)
  {
    this.timestamp = System.currentTimeMillis();

    this.threadName = Thread.currentThread().getName();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    // 0 - current thread class from where stack trace is extracted (Thread.java)
    // 1 - this log event class (LogEvent.java)
    // 2 - logger printer class (LogPrinter.java)
    // 3 - logger implementation class (LogImpl.java)
    // 4 - source class where logger was invoked
    this.logStackElement = stackTrace.length > 4 ? stackTrace[4] : null;

    this.parameters = new HashMap<>();
    LogContextImpl.get().forEach((name, value) -> {
      this.parameters.put(name, value);
    });

    this.loggerName = loggerName;
    this.level = level;
    this.message = message;
    this.arguments = arguments;
  }

  public long getTimestamp()
  {
    return timestamp;
  }

  public String getThreadName()
  {
    return threadName;
  }

  public StackTraceElement getLogStackElement()
  {
    return logStackElement;
  }

  public Map<String, Object> getContextParameters()
  {
    return parameters;
  }

  public String getLoggerName()
  {
    return loggerName;
  }

  public Level getLevel()
  {
    return level;
  }

  public String getMessage()
  {
    return message;
  }

  public Object[] getArguments()
  {
    return arguments;
  }
}
