package com.jslib.std.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Locale;

class GelfRecord
{
  private static final String DEF_VERSION = "1.1";

  private final String message;
  private final Object[] arguments;
  private final long baseTimestamp;

  private String version;
  private String host;
  private String shortMessage;
  private String fullMessage;
  private double timestamp;
  private int level;

  private final LinkedHashMap<String, Object> fields;

  public GelfRecord(String message, Object... arguments)
  {
    this.message = message;
    this.arguments = arguments;

    this.version = DEF_VERSION;
    this.level = SyslogLevel.ALERT.ordinal();

    long timestamp = System.currentTimeMillis();
    this.timestamp = timestamp / 1000D;
    this.baseTimestamp = timestamp / 1000;

    this.fields = new LinkedHashMap<>();
  }

  public String getMessage()
  {
    return message;
  }

  public Object[] getArguments()
  {
    return arguments;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getVersion()
  {
    return version;
  }

  public void setHost(String host)
  {
    this.host = host;
  }

  public String getHost()
  {
    return host;
  }

  public void setShortMessage(String shortMessage)
  {
    this.shortMessage = shortMessage;
  }

  public String getShortMessage()
  {
    return shortMessage;
  }

  public void setFullMessage(String fullMessage)
  {
    this.fullMessage = fullMessage;
  }

  public String getFullMessage()
  {
    return fullMessage;
  }

  public void setTimestamp(double timestamp)
  {
    this.timestamp = timestamp;
  }

  public double getTimestamp()
  {
    return timestamp;
  }

  public long getBaseTimestamp()
  {
    return baseTimestamp;
  }

  public void setLevel(SyslogLevel level)
  {
    this.level = level.ordinal();
  }

  public int getLevel()
  {
    return level;
  }

  public void setField(String name, Object value)
  {
    fields.put(name, value);
  }

  public Object getField(String name)
  {
    return fields.get(name);
  }

  public boolean hasField(String name)
  {
    return fields.containsKey(name);
  }

  public void save(PrintStream printer)
  {
    printer.print('{');

    print(printer, "version", version);
    printer.print(',');

    if(host != null) {
      print(printer, "host", host);
      printer.print(',');
    }

    print(printer, "short_message", shortMessage);
    printer.print(',');

    if(fullMessage != null) {
      print(printer, "full_message", fullMessage);
      printer.print(',');
    }

    print(printer, "timestamp", timestamp);
    printer.print(',');
    print(printer, "level", level);

    for(String name : fields.keySet()) {
      printer.print(',');
      print(printer, '_' + name, fields.get(name));
    }

    printer.print('}');
    printer.println();
    printer.flush();
  }

  private void print(PrintStream printer, String field, Object value)
  {
    if(value == null) {
      return;
    }

    printer.print('"');
    printer.print(field);
    printer.print('"');

    printer.print(':');

    if(value instanceof String) {
      print(printer, (String)value);
    }
    else if(value instanceof Double) {
      printer.print(String.format(Locale.ENGLISH, "%.6f", value));
    }
    else if(value instanceof Integer) {
      printer.print(Integer.toString((int)value));
    }
    else if(value instanceof Long) {
      printer.print(Long.toString((long)value));
    }
    else {
      print(printer, value.toString());
    }
  }

  private void print(PrintStream printer, String string)
  {
    printer.print('"');

    for(int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch(c) {
      case '\\':
      case '"':
        printer.print('\\');
        printer.print(c);
        break;

      case '/':
        printer.print(c);
        break;

      case '\b':
        printer.print("\\b");
        break;

      case '\t':
        printer.print("\\t");
        break;

      case '\n':
        printer.print("\\n");
        break;

      case '\f':
        printer.print("\\f");
        break;

      case '\r':
        printer.print("\\r");
        break;

      default:
        if(c < '\u0020') {
          String unicode = "000" + Integer.toHexString(c);
          printer.print("\\u");
          printer.print(unicode.substring(unicode.length() - 4));
        }
        else {
          printer.print(c);
        }
      }
    }

    printer.print('"');
  }

  public byte[] getBytes()
  {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      save(new PrintStream(bytes, false, "UTF-8"));
    }
    catch(UnsupportedEncodingException ignored) {}
    finally {
      try {
        bytes.close();
      }
      catch(IOException e) {}
    }
    return bytes.toByteArray();
  }
}
