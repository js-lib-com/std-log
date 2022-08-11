package com.jslib.std.log;

import com.jslib.api.log.Log;

class LogImpl implements Log
{
  /** Logger printer. */
  private final LogPrinter printer;
  /** Unique logger name. Used by levels configuration. */
  private final String name;

  /** Configured log level. This level control which logging variants are enabled, e.g. trace, debug, etc. */
  private volatile Level level;

  public LogImpl(LogPrinter printer, String name, Level level)
  {
    this.printer = printer;
    this.name = name;
    this.level = level;
  }

  public String getName()
  {
    return name;
  }

  public void setLevel(Level level)
  {
    this.level = level;
  }

  public Level getLevel()
  {
    return level;
  }

  @Override
  public void trace(String message, Object... args)
  {
    if(level.ordinal() >= Level.TRACE.ordinal()) {
      printer.write(name, Level.TRACE, message, args);
    }
  }

  @Override
  public void debug(String message, Object... args)
  {
    if(level.ordinal() >= Level.DEBUG.ordinal()) {
      printer.write(name, Level.DEBUG, message, args);
    }
  }

  @Override
  public void info(String message, Object... args)
  {
    if(level.ordinal() >= Level.INFO.ordinal()) {
      printer.write(name, Level.INFO, message, args);
    }
  }

  @Override
  public void warn(String message, Object... args)
  {
    if(level.ordinal() >= Level.WARN.ordinal()) {
      printer.write(name, Level.WARN, message, args);
    }
  }

  @Override
  public void warn(Throwable throwable)
  {
    if(level.ordinal() >= Level.WARN.ordinal()) {
      printer.write(name, Level.WARN, throwable(throwable));
    }
  }

  @Override
  public void error(String message, Object... args)
  {
    if(level.ordinal() >= Level.ERROR.ordinal()) {
      printer.write(name, Level.ERROR, message, args);
    }
  }

  @Override
  public void error(Throwable throwable)
  {
    if(level.ordinal() >= Level.ERROR.ordinal()) {
      printer.write(name, Level.ERROR, throwable(throwable));
    }
  }

  @Override
  public void fatal(String message, Object... args)
  {
    if(level.ordinal() >= Level.FATAL.ordinal()) {
      printer.write(name, Level.FATAL, message, args);
    }
  }

  @Override
  public void fatal(Throwable throwable)
  {
    if(level.ordinal() >= Level.FATAL.ordinal()) {
      printer.write(name, Level.FATAL, throwable(throwable));
    }
  }

  @Override
  public void dump(String message, Throwable throwable)
  {
    if(level.ordinal() >= Level.FATAL.ordinal()) {
      printer.write(name, Level.FATAL, message);
      printer.printStackTrace(throwable);
    }
  }

  @Override
  public void dump(Throwable throwable)
  {
    if(level.ordinal() >= Level.FATAL.ordinal()) {
      printer.printStackTrace(throwable);
    }
  }

  private static String throwable(Throwable throwable)
  {
    if(throwable == null) {
      return null;
    }
    if(throwable.getCause() == null) {
      return throwable.getMessage();
    }

    int nestingLevel = 0;
    StringBuilder sb = new StringBuilder();
    for(;;) {
      sb.append(throwable.getClass().getName());
      sb.append(":");
      sb.append(" ");
      if(++nestingLevel == 8) {
        sb.append("...");
        break;
      }
      if(throwable.getCause() == null) {
        String s = throwable.getMessage();
        if(s == null) {
          throwable.getClass().getCanonicalName();
        }
        sb.append(s);
        break;
      }
      throwable = throwable.getCause();
    }
    return sb.toString();
  }
}
