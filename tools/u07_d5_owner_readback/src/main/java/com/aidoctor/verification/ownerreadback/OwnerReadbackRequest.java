package com.aidoctor.verification.ownerreadback;
final class OwnerReadbackRequest {
 final String consultationId,threadId,runId,checkpointId;
 OwnerReadbackRequest(String c,String t,String r,String p){consultationId=c;threadId=t;runId=r;checkpointId=p;}
 boolean valid(){return id(consultationId)&&id(threadId)&&id(runId)&&id(checkpointId);}
 static boolean id(String s){if(s==null||s.length()<1||s.length()>128||!s.equals(s.trim()))return false;for(char c:s.toCharArray())if(Character.isISOControl(c))return false;return true;}
}
