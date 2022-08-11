package com.jslib.std.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class Configuration
{
  private static final String LEVEL_PREFIX = "level.";

  private final Properties properties;
  private final Level rootLevel;
  private final List<LevelConfig> levelsConfig;

  public Configuration()
  {
    this.properties = new Properties();
    try {
      this.properties.load(getClass().getResourceAsStream("/std-log.properties"));
    }
    catch(IOException e) {
      System.err.printf("Fail to load std-log.properties. Root cause: %s%n", e.getMessage());
    }

    this.levelsConfig = new ArrayList<>();
    Level rootLevel = Level.ALL;

    for(Object key : this.properties.keySet()) {
      String propertyName = (String)key;

      // process non level properties

      if(!propertyName.startsWith(LEVEL_PREFIX)) {
        continue;
      }
      String loggerPattern = propertyName.substring(LEVEL_PREFIX.length());
      Level level = Level.valueOf(this.properties.getProperty(propertyName));

      if(loggerPattern.equals("root")) {
        rootLevel = level;
        continue;
      }
      this.levelsConfig.add(new LevelConfig(loggerPattern, level));
    }

    // comparator reverses (x, y) order for descendant sort, longer pattern string firsts
    this.levelsConfig.sort((x, y) -> Integer.compare(y.loggerPattern.length(), x.loggerPattern.length()));
    this.rootLevel = rootLevel;
  }

  public void setLevelListener(LevelListener listener)
  {

  }

  public Level getLevel(String loggerName)
  {
    for(LevelConfig config : levelsConfig) {
      if(loggerName.startsWith(config.loggerPattern)) {
        return config.level;
      }
    }
    return rootLevel;
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
}
