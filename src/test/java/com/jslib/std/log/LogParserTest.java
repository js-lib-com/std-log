package com.jslib.std.log;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.Test;

public class LogParserTest
{
  private LogParser parser;

  @Before
  public void beforeTest()
  {
    parser = new LogParser();
  }
  
  @Test
  public void GivenJsonObject_WhenLog_ThenPreserveOriginalJson() {
    // given
    String json = "{\"tickets0\": {},\"ticketCount\": 0}";
    
    // when
    String message = parser.parse(json);
    
    //then
    assertThat(message, equalTo(json));
  }
}
