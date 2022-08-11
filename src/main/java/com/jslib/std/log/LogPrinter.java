package com.jslib.std.log;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class LogPrinter implements Runnable
{
  /**
   * Milliseconds to wait for log printer close. This timeout is to allow sender thread to flush log events queue. All
   * enqueued log events still existing at timeout moment are lost.
   */
  private static final int PRINTER_CLOSE_TIMEOUT = 8000;

  private static final int QUEUE_READ_TIMEOUT = 6000;

  private final PrintStream printer;

  private final DatagramSocket socket;
  private final InetAddress serverAddress;
  private final int serverPort;

  private final BlockingQueue<Event> eventsQueue;

  private final Thread senderThread;

  public LogPrinter()
  {
    this.printer = System.out;

    DatagramSocket socket = null;
    InetAddress serverAddress = null;
    int serverPort = 12201;
    try {
      socket = new DatagramSocket();
      serverAddress = InetAddress.getByName("log.eonsn.ro");
    }
    catch(IOException e) {}
    if(serverAddress == null) {
      // null socket signals no remote log server; write only to system out
      socket = null;
    }

    this.socket = socket;
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;

    this.eventsQueue = new ArrayBlockingQueue<>(100);

    this.senderThread = new Thread(this);
    this.senderThread.setDaemon(true);
    this.senderThread.setName("std-log-printer");
    this.senderThread.start();
  }

  public void write(String loggerName, Level level, String message, Object... arguments)
  {
    LogEvent logEvent = new LogEvent(loggerName, level, message, arguments);
    for(;;) {
      try {
        eventsQueue.put(logEvent);
        break;
      }
      catch(InterruptedException e) {}
      catch(Exception e) {
        System.err.printf("Fail to enqueue log event. Root cause: %s. Log message lost: %s%n", e.getMessage(), message);
      }
    }
  }

  @Override
  public void run()
  {
    LogEvent logEvent = null;

    for(;;) {
      try {
        if(logEvent == null) {
          Event event = eventsQueue.poll(QUEUE_READ_TIMEOUT, TimeUnit.MILLISECONDS);
          if(event == null) {
            continue;
          }
          if(event instanceof ShutdownEvent) {
            break;
          }
          logEvent = (LogEvent)event;
        }

        GelfRecord record = new GelfRecord();
        // full message is original message, with parameters not resolved
        record.setFullMessage(logEvent.getMessage());

        record.setTimestamp(logEvent.getTimestamp() / 1000.0);
        assert logEvent.getLevel() != Level.OFF;
        record.setLevel(LEVELS.get(logEvent.getLevel()));

        record.setField("log_name", logEvent.getLoggerName());
        record.setField("log_level", logEvent.getLevel().name());
        record.setField("log_thread", logEvent.getThreadName());

        // include log context as custom fields before source code details
        // log context fields have priority and arguments does not override
        logEvent.getContextParameters().forEach((name, value) -> {
          record.setField(name, value);
        });

        StackTraceElement stackElement = logEvent.getLogStackElement();
        if(stackElement != null) {
          record.setField("log_class", stackElement.getClassName());
          record.setField("log_method", stackElement.getMethodName());
          if(stackElement.getLineNumber() >= 0) {
            record.setField("log_file", stackElement.getFileName());
            record.setField("log_line", stackElement.getLineNumber());
          }
        }

        // include arguments as custom fields
        LogParser parser = new LogParser();
        String shortMessage = parser.parse(logEvent.getMessage(), logEvent.getArguments());

        for(String name : parser.getParameters().keySet()) {
          if(!record.hasField(name)) {
            record.setField(name, parser.getParameter(name));
          }
        }

        // short message is message with parameters resolved
        record.setShortMessage(shortMessage);

        final byte[] buffer = record.getBytes();
        if(socket != null) {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
          socket.send(packet);
        }
        printer.write(buffer);
        logEvent = null;
      }
      catch(InterruptedException e) {
        // continue outer while loop that is checking for running flag state
      }
      catch(Exception e) {
        // on exception log event instance is not nullified and no new event extracted from queue
      }
    }
    System.err.printf("Thread %s closed.%n", Thread.currentThread().getName());
  }

  public void printStackTrace(Throwable throwable)
  {
    throwable.printStackTrace(printer);
  }

  public void close()
  {
    eventsQueue.offer(new ShutdownEvent());

    long closeTimeMillis = System.currentTimeMillis() + PRINTER_CLOSE_TIMEOUT;
    while(closeTimeMillis > System.currentTimeMillis()) {
      try {
        senderThread.join(PRINTER_CLOSE_TIMEOUT);
        break;
      }
      catch(InterruptedException e) {}
    }

    printer.close();
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
