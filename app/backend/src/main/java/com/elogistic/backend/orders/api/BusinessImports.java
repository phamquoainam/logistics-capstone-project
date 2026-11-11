package com.elogistic.backend.orders.api;
import java.time.Instant;
import java.util.*;

/** Bounded CSV staging. Preview never creates a preparation or Booking. */
public interface BusinessImports {
    record Error(int rowNumber,String field,String code){}
    record Row(int rowNumber,String state,BusinessPreparations.Input input,List<Error> errors,UUID preparationId,long version){public Row{errors=List.copyOf(errors);}}
    record Job(UUID id,UUID organizationId,UUID facilityId,UUID creatorAccountId,String templateVersion,String state,long version,
               int totalRows,int validRows,int invalidRows,int createdRows,int conflictRows,Instant createdAt,Instant updatedAt,
               List<Row> rows,int page,int size,boolean hasNext){public Job{rows=List.copyOf(rows);}}
    record Page(List<Job> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    Job preview(BusinessPreparations.Access access,String templateVersion,UUID facilityId,String csv,UUID key);
    Job get(BusinessPreparations.Access access,UUID id,int page);
    Page list(BusinessPreparations.Access access,int page);
    Job commit(BusinessPreparations.Access access,UUID id,List<Integer> rows,long expected,UUID key);
    String template(BusinessPreparations.Access access);
    BusinessPreparations.Receipt receipt(BusinessPreparations.Access access,String operation,UUID key);
}
