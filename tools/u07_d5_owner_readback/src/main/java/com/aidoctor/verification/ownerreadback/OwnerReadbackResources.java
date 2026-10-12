package com.aidoctor.verification.ownerreadback;
import com.fasterxml.jackson.databind.*;
import javax.sql.DataSource;import java.sql.*;import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.datasource.*;
final class OwnerReadbackResources {
 static final String PROFILE="NONPRODUCTION_SYNTHETIC",URL="jdbc:mysql://127.0.0.1:33325/u07_owner_readback?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 static final String SOURCE="750770e81af79b764cc4148d7fa856363c89972a",SCHEMA="o1-schema-v1",FIXTURE="o1-fixture-v1";
 final DataSource ds;final DataSourceTransactionManager manager;final JdbcTemplate jdbc;final JsonNode manifest;
 OwnerReadbackResources(String profile,String url){this(profile,url,null);}
 // Package-private override is only for instrumented disposable test DS.
 OwnerReadbackResources(String profile,String url,DataSource instrumented){
  if(!PROFILE.equals(profile)||!URL.equals(url))throw new IllegalArgumentException("CONFIG_REJECTED");
  try{manifest=new ObjectMapper().readTree(getClass().getResourceAsStream("/schema-manifest.json"));}catch(Exception e){throw new IllegalArgumentException("CONFIG_REJECTED",e);}
  if(!SOURCE.equals(manifest.path("source_head").asText())||!SCHEMA.equals(manifest.path("schema_manifest_ref").asText())||manifest.path("tables").size()!=6)throw new IllegalArgumentException("CONFIG_REJECTED");
  ds=instrumented==null?new DriverManagerDataSource(URL,"o1_reader","synthetic-o1-reader"):instrumented;
  manager=new DataSourceTransactionManager(ds);jdbc=new JdbcTemplate(ds);
 }
 void physical(Connection c)throws SQLException{
  if(!DataSourceUtils.isConnectionTransactional(c,ds)||c.getAutoCommit()||c.getTransactionIsolation()!=Connection.TRANSACTION_REPEATABLE_READ||!"u07_owner_readback".equals(c.getCatalog()))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
 }
 void preflight(Connection c)throws SQLException{
  physical(c);if(!c.getMetaData().getDatabaseProductName().equals("MySQL")||!c.getMetaData().getDatabaseProductVersion().startsWith("8.0."))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
  Iterator<Map.Entry<String,JsonNode>> tables=manifest.path("tables").fields();
  while(tables.hasNext()){
   Map.Entry<String,JsonNode> entry=tables.next();String table=entry.getKey();JsonNode expected=entry.getValue();
   List<String> engines=jdbc.query("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",(r,n)->r.getString(1),"u07_owner_readback",table);
   if(engines.size()!=1||!"InnoDB".equals(engines.get(0)))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
   List<Map<String,Object>> cols=jdbc.queryForList("SELECT COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? ORDER BY ORDINAL_POSITION","u07_owner_readback",table);
   if(cols.size()!=expected.path("columns").size())throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
   for(Map<String,Object> actual:cols){JsonNode col=expected.path("columns").path((String)actual.get("COLUMN_NAME"));
    if(col.isMissingNode()||!col.path("type").asText().equals(actual.get("DATA_TYPE"))||!col.path("nullable").asText().equals(actual.get("IS_NULLABLE"))||(!col.path("length").isNull()&&((Number)actual.get("CHARACTER_MAXIMUM_LENGTH")).longValue()!=col.path("length").asLong()))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
   }
   Map<String,List<String>> indices=new TreeMap<>();for(Map<String,Object> row:jdbc.queryForList("SELECT INDEX_NAME,COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND NON_UNIQUE=0 ORDER BY INDEX_NAME,SEQ_IN_INDEX","u07_owner_readback",table))indices.computeIfAbsent((String)row.get("INDEX_NAME"),k->new ArrayList<>()).add((String)row.get("COLUMN_NAME"));
   List<String> primary=indices.remove("PRIMARY"),wantedPrimary=new ArrayList<>();for(JsonNode column:expected.path("primary_key"))wantedPrimary.add(column.asText());
   if(primary==null||wantedPrimary.isEmpty()||!primary.equals(wantedPrimary))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
   List<List<String>> observed=new ArrayList<>(indices.values());List<List<String>> wanted=new ArrayList<>();for(JsonNode key:expected.path("unique")){List<String> list=new ArrayList<>();for(JsonNode v:key)list.add(v.asText());wanted.add(list);}
   Comparator<List<String>> order=Comparator.comparing(Object::toString);observed.sort(order);wanted.sort(order);if(!observed.equals(wanted))throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
  }
 }
}
