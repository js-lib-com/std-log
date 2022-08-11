package com.jslib.std.log;

enum Level
{
  // enumeration constants matters; keep it as it is

  /** Log is disabled. */
  OFF,

  /** Very severe error events that will presumably lead the application to abort. */
  FATAL,

  /** Error events that might still allow the application to continue running. */
  ERROR,

  /** Potentially harmful situations. */
  WARN,

  /** Informational messages that highlight the progress of the application at coarse-grained level. */
  INFO,

  /** Fine-grained informational events that are most useful to debug an application. */
  DEBUG,

  /** The same as {@link #DEBUG} but with finer granularity. */
  TRACE,

  /** All levels are enabled. */
  ALL
}
