package com.jslib.std.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jslib.api.log.DefaultLogTransaction;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogConfig;
import com.jslib.api.log.LogContext;
import com.jslib.api.log.LogProvider;
import com.jslib.api.log.LogTransaction;

public class LogProviderImpl implements LogProvider, Configuration.LevelListener
{
  private final Configuration configuration;
  private final LogPrinter printer;
  private final LogTransaction transaction;
  private final List<LogImpl> loggers;

  public LogProviderImpl() throws IOException
  {
    this.configuration = new Configuration();
    this.printer = new LogPrinter(this.configuration);
    this.transaction = this.configuration.isLogTransaction() ? printer : DefaultLogTransaction.instance;
    this.configuration.setLevelListener(this);

    this.loggers = new ArrayList<>();
  }

  @Override
  public LogConfig getLogConfig()
  {
    return configuration;
  }

  @Override
  public LogContext getLogContext()
  {
    return LogContextImpl.get();
  }

  @Override
  public LogTransaction getLogTransaction()
  {
    return transaction;
  }

  @Override
  public Log getLogger(String loggerName)
  {
    return new LogImpl(printer, loggerName, configuration.getLoggerLevel(loggerName));
  }

  @Override
  public void close()
  {
    printer.close();
  }

  @Override
  public void onLevelChange()
  {
    loggers.forEach(logger -> logger.setLevel(configuration.getLoggerLevel(logger.getName())));
  }
}
