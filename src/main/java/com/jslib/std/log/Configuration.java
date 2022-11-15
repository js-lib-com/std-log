package com.jslib.std.log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.jslib.api.log.Level;
import com.jslib.api.log.LogConfig;

class Configuration implements LogConfig
{
  private static final String LEVEL_PREFIX = "level.";

  private Level rootLevel;
  private List<LevelConfig> levelsConfig;
  private URI serverAddress;
  private String consolePrinter;

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

    for(Object key : properties.keySet()) {
      String propertyName = (String)key;

      // process non level properties

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

    String serverAddress = properties.getProperty("log.server");
    if(serverAddress != null) {
      this.serverAddress = URI.create(serverAddress);
    }
    
    this.consolePrinter = properties.getProperty("console.printer", "stdout");
  }

  private static InputStream propertiesStream() throws FileNotFoundException
  {
    String propertiesPath = System.getProperty("STD_LOG");
    if(propertiesPath != null) {
      return new FileInputStream(propertiesPath);
    }
    return Configuration.class.getResourceAsStream("/std-log.properties");
  }

  public void setLevelListener(LevelListener listener)
  {

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
