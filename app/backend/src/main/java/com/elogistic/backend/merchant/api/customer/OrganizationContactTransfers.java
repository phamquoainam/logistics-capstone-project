package com.elogistic.backend.merchant.api.customer;
import java.time.Instant;
import java.util.*;
/** Bounded, explicit staging. Preview never creates a contact. */
public interface OrganizationContactTransfers {
    record Row(int rowNumber,String state,OrganizationContacts.Input input,List<String> errors,UUID contactId){public Row{errors=List.copyOf(errors);}@Override public String toString(){return "ContactImportRow[redacted]";}}
    record Job(UUID id,UUID organizationId,UUID facilityId,UUID creatorAccountId,String templateVersion,long version,int totalRows,int validRows,int invalidRows,int createdRows,Instant createdAt,Instant updatedAt,List<Row> rows,int page,int size,boolean hasNext){public Job{rows=List.copyOf(rows);}@Override public String toString(){return "ContactImportJob[redacted]";}}
    record Page(List<Job> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Export(UUID exportId,UUID organizationId,Instant generatedAt,String source,int rowCount,List<OrganizationContacts.Contact> rows){public Export{rows=List.copyOf(rows);}@Override public String toString(){return "ContactExport[redacted]";}}
    Job preview(OrganizationContacts.Access access,String template,UUID facility,String csv,UUID key);
    Job get(OrganizationContacts.Access access,UUID id,int page);
    Page list(OrganizationContacts.Access access,int page);
    Job commit(OrganizationContacts.Access access,UUID id,List<Integer> rows,long version,UUID key);
    Export export(OrganizationContacts.Access access,OrganizationContacts.Query filters);
}
