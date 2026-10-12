package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import java.sql.Connection;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Test-only fault instrumentation, not an application SQL firewall. */
public final class StageBPostWriteFaultJdbcTemplate extends JdbcTemplate {
 private static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent",
  "u06_delivery_attempt","u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 public interface AfterWriteObserver {void observe(int ordinal);}
 private final int failureOrdinal;
 private final AfterWriteObserver observer;
 private int successes;
 private long writerConnectionId;
 private boolean transactionActiveAtFault;
 public StageBPostWriteFaultJdbcTemplate(DataSource ds,int n,AfterWriteObserver observer){
  super(ds);
  if(n<0||n>6)throw new IllegalArgumentException("INVALID_FAULT_ORDINAL");
  failureOrdinal=n;this.observer=observer;
 }
 @Override public int update(String sql,Object... args){
  if(sql==null)throw new IllegalStateException("STAGE_B_SQL_NULL_DENIED");
  String normalized=sql.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
  if(successes>=TABLES.length || !normalized.startsWith("insert into "+TABLES[successes]+" "))
   throw new IllegalStateException("STAGE_B_UNEXPECTED_SQL_ORDER");
  if(!TransactionSynchronizationManager.isActualTransactionActive())
   throw new IllegalStateException("STAGE_B_SQL_OUTSIDE_TX_DENIED");
  Connection c=DataSourceUtils.getConnection(getDataSource());
  if(!DataSourceUtils.isConnectionTransactional(c,getDataSource()))
   throw new IllegalStateException("STAGE_B_WRITER_NOT_BOUND");
  int changed=super.update(sql,args);
  if(changed!=1)throw new IllegalStateException("STAGE_B_INSERT_COUNT_UNEXPECTED");
  successes++;
  if(successes==failureOrdinal){
   writerConnectionId=Long.valueOf(super.queryForObject("SELECT CONNECTION_ID()",Long.class));
   transactionActiveAtFault=TransactionSynchronizationManager.isActualTransactionActive();
   if(observer!=null)observer.observe(successes);
   throw new StageBInjectedAfterWriteFailure(successes);
  }
  return changed;
 }
 public int actualWrites(){return successes;}
 public long writerConnectionId(){return writerConnectionId;}
 public boolean transactionActiveAtFault(){return transactionActiveAtFault;}
 public static final class StageBInjectedAfterWriteFailure extends RuntimeException{
  public final int ordinal;
  public StageBInjectedAfterWriteFailure(int n){super("STAGE_B_POST_WRITE_FAULT_"+n);ordinal=n;}
 }
}
