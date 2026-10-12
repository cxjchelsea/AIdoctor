package com.aidoctor.verification.ownerreadback;
import java.util.concurrent.*;import org.junit.jupiter.api.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.datasource.*;import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;import static com.aidoctor.verification.ownerreadback.WaitChainObservation.Status.*;
class SnapshotConcurrencyTest {
 @BeforeAll static void setup()throws Exception{WaitChainReaderTest.setup();}
 @Test void independentWriterCommitsAfterRootButReaderRetainsSingleSnapshot()throws Exception{
  WaitChainReaderTest.Fixture f=new WaitChainReaderTest.Fixture();CountDownLatch rootRead=new CountDownLatch(1),committed=new CountDownLatch(1);ExecutorService pool=Executors.newSingleThreadExecutor();
  try{Future<?> writer=pool.submit(()->{await(rootRead);JdbcTemplate db=new JdbcTemplate(new DriverManagerDataSource(OwnerReadbackResources.URL,"o1_writer","synthetic-o1-writer"));new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource())).execute(s->{db.update("UPDATE clinical_consultation SET lifecycle_status='ACTIVE',current_wait_effect_id=NULL,row_version=row_version+1 WHERE consultation_id=?",f.c);db.update("UPDATE clinical_runtime_thread_state SET runtime_status='ACTIVE',current_run_id=NULL,current_wait_checkpoint_id=NULL,current_wait_effect_id=NULL,row_version=row_version+1 WHERE thread_id=?",f.t);return null;});committed.countDown();});
   WaitChainReader reader=new WaitChainReader(WaitChainReaderTest.resources(),f.authorize(f.request()),()->{rootRead.countDown();await(committed);});WaitChainObservation old=reader.read(f.request());writer.get(8,TimeUnit.SECONDS);WaitChainReaderTest.expect(old,MATCH,"WAIT_CHAIN_MATCH");assertEquals(1L,old.payload.get("consultationRowVersion"));WaitChainReaderTest.expect(WaitChainReaderTest.read(f,f.request()),STALE,"NO_CURRENT_WAIT");
  }finally{rootRead.countDown();committed.countDown();pool.shutdownNow();}
 }
 static void await(CountDownLatch latch){try{if(!latch.await(6,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
