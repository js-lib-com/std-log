package com.jslib.std.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Locale;

class GelfRecord
{
  private static final String DEF_VERSION = "1.1";

  private String version;
  private String host;
  private String shortMessage;
  private String fullMessage;
  private double timestamp;
  private int level;

  private final LinkedHashMap<String, Object> fields;

  public GelfRecord()
  {
    this.version = DEF_VERSION;
    this.timestamp = System.currentTimeMillis() / 1000.0;
    this.level = SyslogLevel.ALERT.ordinal();

    this.fields = new LinkedHashMap<>();
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

  public Object getFiled(String name)
  {
    return fields.get(name);
  }

  public boolean hasField(String name)
  {
    return fields.containsKey(name);
  }

  public void save(PrintStream printer) throws IOException
  {
    printer.write('{');

    write(printer, "version", version);
    printer.write(',');
    
    if(host != null) {
      write(printer, "host", host);
      printer.write(',');
    }
    
    write(printer, "short_message", shortMessage);
    printer.write(',');

    if(fullMessage != null) {
      write(printer, "full_message", fullMessage);
      printer.write(',');
    }

    write(printer, "timestamp", timestamp);
    printer.write(',');
    write(printer, "level", level);

    for(String name : fields.keySet()) {
      printer.write(',');
      write(printer, '_' + name, fields.get(name));
    }

    printer.write('}');
    printer.println();
  }

  private void write(PrintStream printer, String field, Object value) throws IOException
  {
    if(value == null) {
      return;
    }

    printer.write('"');
    printer.write(field.getBytes());
    printer.write('"');

    printer.write(':');

    if(value instanceof String) {
      printer.write('"');
      printer.write(((String)value).getBytes());
      printer.write('"');
    }
    else if(value instanceof Double) {
      printer.write(String.format(Locale.ENGLISH, "%.4f", value).getBytes());
    }
    else if(value instanceof Integer) {
      printer.write(Integer.toString((int)value).getBytes());
    }
    else {
      printer.write('"');
      printer.write(value.toString().getBytes());
      printer.write('"');
    }
  }

  public byte[] getBytes()
  {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      save(new PrintStream(bytes));
    }
    catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    finally {
      try {
        bytes.close();
      }
      catch(IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return bytes.toByteArray();
  }
}
