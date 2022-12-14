package com.jslib.std.log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jslib.api.log.Level;
import com.jslib.api.log.LogConfig;

class Configuration implements LogConfig
{
  private static final String SYSTEM_PROPERTY = "STD_LOG";
  private static final String RESOURCE_FILE = "/std-log.properties";

  private static final String CONTEXT_PARAMETERS_PREFIX = "context.";

  private static final String PROP_LOG_TRANSACTION = "log.transaction";
  private static final String PROP_LOG_SERVER = "log.server";
  private static final String PROP_CONSOLE_PRINTER = "console.printer";

  private static final String LEVEL_PREFIX = "level.";
  private static final String STDOUT = "stdout";

  private boolean logTransaction;
  private Level rootLevel;
  private List<LevelConfig> levelsConfig;
  private URI serverAddress;
  private String consolePrinter;
  private final Map<String, String> contextParameters;

  public Configuration()
  {
    Properties properties = new Properties();

    try (InputStream propertiesStream = propertiesStream()) {
      properties.load(propertiesStream);
    }
    catch(IOException e) {
      System.err.printf("Fail to load configuration properties. Root cause: %s: %s%n", e.getClass().getCanonicalName(), e.getMessage());
    }

    this.levelsConfig = new ArrayList<>();
    Level rootLevel = Level.ALL;

    this.contextParameters = new HashMap<>();

    for(Object key : properties.keySet()) {
      String propertyName = (String)key;
      if(propertyName.startsWith(CONTEXT_PARAMETERS_PREFIX)) {
        this.contextParameters.put(propertyName.substring(CONTEXT_PARAMETERS_PREFIX.length()), properties.getProperty(propertyName));
        continue;
      }

      if(!propertyName.startsWith(LEVEL_PREFIX)) {
        continue;
      }
      String loggerPattern = propertyName.substring(LEVEL_PREFIX.length());
      Level level = Level.valueOf(properties.getProperty(propertyName));

      if(loggerPattern.equals("root")) {
        rootLevel = level;
        continue;
      }
      this.levelsConfig.add(new LevelConfig(loggerPattern, level));
    }

    // comparator reverses (x, y) order for descendant sort, longer pattern string firsts
    this.levelsConfig.sort((x, y) -> Integer.compare(y.loggerPattern.length(), x.loggerPattern.length()));
    this.rootLevel = rootLevel;

    this.logTransaction = Boolean.parseBoolean(properties.getProperty(PROP_LOG_TRANSACTION));

    String serverAddress = properties.getProperty(PROP_LOG_SERVER);
    if(serverAddress != null) {
      this.serverAddress = URI.create(serverAddress);
    }

    this.consolePrinter = properties.getProperty(PROP_CONSOLE_PRINTER, STDOUT);
  }

  private static InputStream propertiesStream() throws FileNotFoundException
  {
    String propertiesPath = System.getProperty(SYSTEM_PROPERTY);
    if(propertiesPath != null) {
      return new FileInputStream(propertiesPath);
    }
    return Configuration.class.getResourceAsStream(RESOURCE_FILE);
  }

  public Map<String, String> getContextParameters()
  {
    return contextParameters;
  }

  public void setLevelListener(LevelListener listener)
  {

  }

  public boolean isLogTransaction()
  {
    return logTransaction;
  }

  public void setLogTransaction(boolean logTransaction)
  {
    this.logTransaction = logTransaction;
  }

  @Override
  public void setServerAddress(URI address)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public URI getServerAddress()
  {
    return serverAddress;
  }

  @Override
  public void setRootLevel(Level level)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public Level getRootLevel()
  {
    return rootLevel;
  }

  @Override
  public void setLoggerLevel(String loggerName, Level level)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public Level getLoggerLevel(String loggerName)
  {
    for(LevelConfig config : levelsConfig) {
      if(loggerName.startsWith(config.loggerPattern)) {
        return config.level;
      }
    }
    return rootLevel;
  }

  @Override
  public void clearLoggerLevel(String loggerName)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void commit()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void setFilter(String filter)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public String getFilter()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void clearFilter()
  {
    // TODO Auto-generated method stub

  }

  @FunctionalInterface
  public interface LevelListener
  {
    void onLevelChange();
  }

  private static class LevelConfig
  {
    public final String loggerPattern;
    public final Level level;

    public LevelConfig(String loggerPattern, Level level)
    {
      this.loggerPattern = loggerPattern;
      this.level = level;
    }
  }

  public String getConsolePrinter()
  {
    return consolePrinter;
  }
}
