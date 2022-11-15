package com.jslib.std.log;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogContext;
import com.jslib.api.log.LogFactory;

public class LogTest
{
  private static final Log log = LogFactory.getLog(LogTest.class);

  @BeforeClass
  public static void beforeClass()
  {
    Random random = new Random();
    LogContext context = LogFactory.getLogContext();
    context.put("trace_id", Integer.toHexString(random.nextInt()));
    context.put("app", "std-log");
  }

  @AfterClass
  public static void afterClass() {
    LogFactory.close();
  }
  
  @Test
  public void test()
  {
    log.trace("trace test {person}", "John Doe");
    log.debug("debug test {person}", "Tom Joad");
    log.info("info test {person}", "Paul Atreides");
    log.warn("warn test {person}", "Adam Trask");
    log.error("error test {person}", "Don Quijote");
    log.fatal("fatal test {person}", "Quasimodo");
  }

  @Test
  public void anonymousParameter() throws InterruptedException {
    log.trace("trace test {}", "John Doe");
  }
  
  @Test
  public void secondTest()
  {
    for(int i = 0; i < 10; i++) {
      log.trace("{index} trace test {person}", i, "John Doe");
      log.debug("{index} debug test {person}", i, "Tom Joad");
      log.info("{index} info test {person}", i, "Paul Atreides");
      log.warn("{index} warn test {person}", i, "Adam Trask");
      log.error("{index} error test {person} of age {age}", i, "Don Quijote", 64);
      log.fatal("{index} fatal test {herro} with age of {age}. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", i, "Quasimodo", 26);
    }
  }
}
