package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import javax.sql.DataSource;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;

/** Test-only hook: execute real SQL first, then inject an unchecked failure. */
public final class StageBPostWriteFaultJdbcTemplate extends JdbcTemplate {
 private static final String[] TABLES={
  "u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
  "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 private final int failureOrdinal;
 private int successes;
 public StageBPostWriteFaultJdbcTemplate(DataSource ds,int n){
  super(ds);
  if(n<0||n>6)throw new IllegalArgumentException("INVALID_FAULT_ORDINAL");
  failureOrdinal=n;
 }
 @Override
 public int update(String sql,Object... args){
  String normalized=sql.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
  if(normalized.startsWith("insert into u06_delivery_")){
   if(successes>=TABLES.length || !normalized.startsWith("insert into "+TABLES[successes]+" "))
    throw new IllegalStateException("STAGE_B_UNEXPECTED_SQL_ORDER");
   int count=super.update(sql,args);
   successes++;
   if(failureOrdinal==successes)throw new StageBInjectedAfterWriteFailure(successes);
   return count;
  }
  return super.update(sql,args);
 }
 public int actualWrites(){return successes;}
 public static final class StageBInjectedAfterWriteFailure extends RuntimeException{
  public final int ordinal;
  public StageBInjectedAfterWriteFailure(int n){super("STAGE_B_POST_WRITE_FAULT_"+n);ordinal=n;}
 }
}
