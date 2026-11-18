package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.OrganizationTeam.*;
import java.util.*;

/** Explicit capabilities, never role-based implicit grants. Mirrors the approved OTA manifest. */
public final class OrganizationTeamPolicy {
    private OrganizationTeamPolicy(){}
    public static final Set<String> TEAM=Set.of("team.read","team.invite","team.access.manage","team.membership.manage","team.audit.read");
    public static final List<String> EXISTING=List.of("dashboard.read","dashboard.finance.read","dashboard.staff.read","shipment.draft.read","shipment.draft.write","shipment.import","shipment.template.manage","shipment.prepare","shipment.quote","shipment.book","shipment.label.read","shipment.handover.read","shipment.handover.manage","facility.read","facility.manage","defaults.manage","shipment.export","shipment.export.pii","shipment.assign","shipment.reports.read","shipment.finance.read","contacts.read","contacts.write","contacts.import","contacts.export","shipment.actions.manage","shipment.actions.respond");
    private static final Set<String> NO_MANAGER=Set.of("dashboard.finance.read","shipment.finance.read","shipment.export.pii");
    private static final Set<String> NO_OPERATOR=Set.of("dashboard.staff.read","shipment.assign","facility.manage","defaults.manage","shipment.template.manage","shipment.handover.manage","shipment.actions.manage");
    public static final Set<String> FACILITY=Set.of("shipment.draft.read","shipment.draft.write","shipment.import","shipment.template.manage","shipment.prepare","shipment.quote","shipment.book","shipment.label.read","shipment.export","shipment.export.pii","shipment.assign","shipment.reports.read","shipment.finance.read","contacts.read","contacts.write","contacts.import","contacts.export","shipment.actions.manage","shipment.actions.respond","facility.read","facility.manage","shipment.handover.read","shipment.handover.manage");
    public static List<String> delegable(String role){
        if(role==null||!Set.of("OWNER","MANAGER","OPERATOR").contains(role))throw fail("VALIDATION_FAILED");
        return EXISTING.stream().filter(p->role.equals("OWNER")||!NO_MANAGER.contains(p)&&(role.equals("MANAGER")||!NO_OPERATOR.contains(p))).toList();
    }
    public static List<String> allowed(String role){var result=new ArrayList<>(delegable(role));if(role.equals("OWNER"))result.addAll(TEAM.stream().sorted().toList());else if(role.equals("MANAGER"))result.addAll(List.of("team.read","team.invite","team.audit.read"));return List.copyOf(result);}
    public static List<Grant> grants(List<Grant> grants){
        if(grants==null||grants.isEmpty()||grants.size()>200||new HashSet<>(grants).size()!=grants.size())throw fail("VALIDATION_FAILED");
        for(var g:grants){if(g==null||g.permission()==null||g.scope()==null||(!EXISTING.contains(g.permission())&&!TEAM.contains(g.permission()))||!Set.of("ORGANIZATION","FACILITY").contains(g.scope())
          ||g.scope().equals("ORGANIZATION")!=(g.facilityId()==null)||g.facilityId()!=null&&(g.facilityId().equals(new UUID(0,0))||!FACILITY.contains(g.permission())))throw fail("VALIDATION_FAILED");}
        if(!grants.contains(new Grant("dashboard.read","ORGANIZATION",null)))throw fail("VALIDATION_FAILED");
        // Preserve the read dependencies already enforced by V24/V29/V32.
        // Reject invalid requests before SQL; database triggers remain authoritative.
        for(var g:grants){var required=new HashSet<String>();
            if(g.permission().startsWith("shipment.")&&!g.permission().equals("shipment.draft.read"))required.add("shipment.draft.read");
            if(Set.of("contacts.write","contacts.import","contacts.export").contains(g.permission()))required.add("contacts.read");
            if(g.permission().equals("contacts.import"))required.add("contacts.write");
            if(g.permission().equals("facility.manage"))required.add("facility.read");
            if(g.permission().equals("shipment.handover.manage"))required.add("shipment.handover.read");
            for(String permission:required)if(grants.stream().noneMatch(read->read.permission().equals(permission)&&(read.scope().equals("ORGANIZATION")||Objects.equals(read.facilityId(),g.facilityId()))))throw fail("VALIDATION_FAILED");
        }
        return grants.stream().sorted(Comparator.comparing(Grant::permission).thenComparing(Grant::scope).thenComparing(g->Objects.toString(g.facilityId(),""))).toList();
    }
    public static void delegation(String actorRole,String targetRole,List<Grant> requested,java.util.function.BiPredicate<String,UUID> has){
        if(!Set.of("MANAGER","OPERATOR").contains(targetRole)||!actorRole.equals("OWNER")&&!(actorRole.equals("MANAGER")&&targetRole.equals("OPERATOR")))throw fail("TEAM_DELEGATION_DENIED");
        for(var g:grants(requested))if(!delegable(targetRole).contains(g.permission())||!has.test(g.permission(),g.facilityId()))throw fail("TEAM_DELEGATION_DENIED");
    }
    public static String reason(String value){if(value==null||value.codePointCount(0,value.length())>300||value.trim().isEmpty()||value.codePoints().anyMatch(Character::isISOControl))throw fail("VALIDATION_FAILED");return value.trim();}
    public static void transition(String role,String from,String operation){if(role.equals("OWNER"))throw fail("LAST_OWNER_PROTECTED");boolean ok=switch(operation){case "SUSPEND"->from.equals("ACTIVE");case "RESUME"->from.equals("SUSPENDED");case "REVOKE_MEMBER"->Set.of("ACTIVE","SUSPENDED").contains(from);case "ACCESS"->from.equals("ACTIVE");default->false;};if(!ok)throw fail("TEAM_STATE_CONFLICT");}
    public static Failure fail(String code){return new Failure(code);}
}
