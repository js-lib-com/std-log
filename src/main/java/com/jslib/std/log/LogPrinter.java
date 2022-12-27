package com.jslib.std.log;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jslib.api.log.Level;
import com.jslib.api.log.LogTransaction;

public class LogPrinter implements LogTransaction, Runnable
{
  /**
   * Milliseconds to wait for log printer close. This timeout is to allow sender thread to flush log events queue. All
   * enqueued log events still existing at timeout moment are lost.
   */
  private static final int PRINTER_CLOSE_TIMEOUT = 8000;

  private static long messageBaseTimestamp;
  private static int messageOffset;

  private final Map<String, String> contextParameters;
  private final PrintStream printer;

  private final DatagramSocket socket;
  private final InetAddress serverAddress;
  private final int serverPort;

  private static final class Transaction
  {
    final BlockingQueue<GelfRecord> queue = new LinkedBlockingQueue<>();
    boolean active;
  }

  private final ThreadLocal<Transaction> threadTransaction;
  private final BlockingQueue<GelfRecord> logsQueue;

  private final Thread senderThread;
  private final AtomicBoolean running;

  public LogPrinter(Configuration configuration) throws IOException
  {
    this.contextParameters = configuration.getContextParameters();

    PrintStream printer;
    switch(configuration.getConsolePrinter()) {
    case "stdout":
      printer = System.out;
      break;
    case "stderr":
      printer = System.err;
      break;
    default:
      printer = null;
    }
    this.printer = printer;

    URI serverAddress = configuration.getServerAddress();
    if(serverAddress != null) {
      this.serverPort = serverAddress.getPort();
      this.serverAddress = InetAddress.getByName(serverAddress.getHost());
      this.socket = new DatagramSocket();
    }
    else {
      this.serverPort = 0;
      this.serverAddress = null;
      this.socket = null;
    }

    this.threadTransaction = new ThreadLocal<>();
    this.logsQueue = new LinkedBlockingQueue<>();

    this.senderThread = new Thread(this);
    this.running = new AtomicBoolean();
    this.senderThread.setDaemon(true);
    this.senderThread.setName("std-log-printer");
    this.senderThread.start();
  }

  @Override
  public void beginTransaction()
  {
    Transaction transaction = threadTransaction.get();
    if(transaction == null) {
      transaction = new Transaction();
      threadTransaction.set(transaction);
    }
    transaction.active = true;
  }

  @Override
  public void commitTransaction()
  {
    Transaction transaction = threadTransaction.get();
    if(transaction == null) {
      return;
    }

    Iterator<GelfRecord> records = transaction.queue.iterator();
    while(records.hasNext()) {
      GelfRecord record = records.next();
      Object value = record.getField("log_level_ordinal");
      if(value != null && value instanceof Integer) {
        Integer level = (Integer)value;
        if(level <= Level.INFO.ordinal()) {
          enqueue(logsQueue, record);
        }
      }
    }

    transaction.queue.clear();
    transaction.active = false;
  }

  @Override
  public void rollbackTransaction()
  {
    Transaction transaction = threadTransaction.get();
    if(transaction == null) {
      return;
    }

    Iterator<GelfRecord> records = transaction.queue.iterator();
    while(records.hasNext()) {
      enqueue(logsQueue, records.next());
    }

    transaction.queue.clear();
    transaction.active = false;
  }

  public void write(String loggerName, Level level, String message, Object... arguments)
  {
    assert level != Level.OFF;

    GelfRecord record = new GelfRecord(message, arguments);

    record.setLevel(LEVELS.get(level));

    if(messageBaseTimestamp < record.getBaseTimestamp()) {
      messageOffset = -1;
      messageBaseTimestamp = record.getBaseTimestamp();
    }
    ++messageOffset;

    record.setField("log_id", (messageBaseTimestamp << 16) + (messageOffset & 0xFFFF));
    record.setField("log_name", loggerName);
    record.setField("log_level", level.name());
    record.setField("log_level_ordinal", level.ordinal());
    record.setField("log_thread", Thread.currentThread().getName());

    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    // 0 - current thread class from where stack trace is extracted (Thread.java)
    // 1 - this logger printer class (LogPrinter.java)
    // 2 - logger implementation class (LogImpl.java)
    // 3 - source class where logger was invoked
    if(stackTrace.length > 3) {
      StackTraceElement stackElement = stackTrace[3];
      record.setField("log_class", stackElement.getClassName());
      record.setField("log_method", stackElement.getMethodName());
      if(stackElement.getLineNumber() >= 0) {
        record.setField("log_file", stackElement.getFileName());
        record.setField("log_line", stackElement.getLineNumber());
      }
    }

    LogContextImpl.get().forEach((name, value) -> {
      record.setField(name, value);
    });

    String traceTimestamp = (String)record.getField("trace_timestamp");
    if(traceTimestamp != null) {
      try {
        record.setField("trace_offset", (System.nanoTime() - Long.parseLong(traceTimestamp)) / 1000.0D);
      }
      catch(Throwable unused) {}
    }

    Transaction transaction = threadTransaction.get();
    if(transaction != null && transaction.active) {
      enqueue(transaction.queue, record);
    }
    else {
      enqueue(logsQueue, record);
    }
  }

  private static void enqueue(BlockingQueue<GelfRecord> queue, GelfRecord record)
  {
    for(;;) {
      try {
        queue.put(record);
        break;
      }
      catch(InterruptedException e) {}
      catch(Exception e) {
        System.err.printf("Fail to enqueue log event. Root cause: %s. Log message lost: %s%n", e.getMessage(), record.getMessage());
      }
    }
  }

  @Override
  public void run()
  {
    GelfRecord record = null;
    running.set(true);

    while(running.get() || !logsQueue.isEmpty()) {
      try {
        record = logsQueue.take();
      }
      catch(InterruptedException e) {
        continue;
      }

      try {
        // include arguments as custom fields
        LogParser parser = new LogParser();
        String message = parser.parse(record.getMessage(), record.getArguments());

        for(String name : contextParameters.keySet()) {
          if(!record.hasField(name)) {
            record.setField(name, contextParameters.get(name));
          }
        }
        
        for(String name : parser.getParameters().keySet()) {
          if(!record.hasField(name)) {
            record.setField(name, parser.getParameter(name));
          }
        }

        // short message is message with parameters resolved
        record.setShortMessage(message);
        // GELF full message is for, usually large, extra data like context dump
        record.setFullMessage(parser.getMessageExtra());
      }
      catch(Throwable t) {
        if(printer != null) {
          printer.printf("Fail to process GELF record: %s. Exception: %s: %s%n", record.getMessage(), t.getClass().getCanonicalName(), t.getMessage());
        }
        continue;
      }

      final byte[] buffer = record.getBytes();
      try {
        if(socket != null) {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
          socket.send(packet);
        }
      }
      catch(Throwable t) {}

      if(printer != null) {
        printer.write(buffer, 0, buffer.length);
      }
    }

    System.err.printf("Thread %s closed.%n", Thread.currentThread().getName());
  }

  public void close()
  {
    running.set(false);
    senderThread.interrupt();

    long closeTimeMillis = System.currentTimeMillis() + PRINTER_CLOSE_TIMEOUT;
    while(closeTimeMillis > System.currentTimeMillis()) {
      try {
        senderThread.join(PRINTER_CLOSE_TIMEOUT);
        break;
      }
      catch(InterruptedException e) {}
    }

    if(socket != null) {
      socket.close();
    }
  }

  private static final Map<Level, SyslogLevel> LEVELS = new HashMap<>();
  static {
    LEVELS.put(Level.FATAL, SyslogLevel.EMERGENCY);
    LEVELS.put(Level.ERROR, SyslogLevel.ERROR);
    LEVELS.put(Level.WARN, SyslogLevel.WARNING);
    LEVELS.put(Level.INFO, SyslogLevel.INFORMATIONAL);
    LEVELS.put(Level.DEBUG, SyslogLevel.CRITICAL);
    LEVELS.put(Level.TRACE, SyslogLevel.DEBUG);
    LEVELS.put(Level.ALL, SyslogLevel.ALERT);
  }
}
