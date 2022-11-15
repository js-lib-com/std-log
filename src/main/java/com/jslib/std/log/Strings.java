package com.jslib.std.log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Strings
{
  public static String throwable(Throwable throwable)
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

  public static String stackTrace(Throwable throwable)
  {
    StringWriter stackTrace = new StringWriter();
    try (PrintWriter printer = new PrintWriter(stackTrace)) {
      throwable.printStackTrace(printer);
    }
    return stackTrace.toString();
  }
}
