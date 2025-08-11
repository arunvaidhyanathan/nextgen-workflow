# How Cerbos Works

## The Core Principle: Cerbos is Stateless

You are correct, you do not create principals inside Cerbos.

Cerbos itself stores no data about your users, resources, or their relationships. It is a pure, stateless decision engine. Its only job is to answer one question, over and over again:

"Given this principal (the user), trying to perform this action, on this resource... is it allowed?"

The "principal," "action," and "resource" are all provided at the time of the check. Cerbos holds the rules (your YAML policies), but your application holds the data.

## How the Check Works: The Role of the Entitlement Service

The Entitlement Service is the crucial intermediary. It is the "Cerbos Principal Builder." It queries your database (the source of truth) to construct the rich Principal and Resource objects that Cerbos needs to make a decision.

Here is the detailed flow for a typical authorization check, for example, when user bob.hr tries to claim a task.

## Flow Diagram: Authorization Check for "Claim Task"

```
+-----------------------------------+
| 1. CMS Domain Service             |
|    - User 'bob.hr' tries to       |
|      claim Task 'task-123'        |
|    - Calls its EntitlementGateway |
+------------------+----------------+
                   |
                   | 2. POST /api/entitlements/check
                   |    Payload:
                   |    {
                   |      "principalId": "bob.hr",
                   |      "resource": {
                   |        "kind": "OneCMS::Process_CMS_Workflow_Updated",
                   |        "id": "proc-inst-456",
                   |        "attr": { ... task & case data ... }
                   |      },
                   |      "action": "claim_task"
                   |    }
                   |
                   v
+------------------+----------------+
| 3. Entitlement Service            |
|    (The Principal Builder)        |
+-----------------------------------+
    |
    | 3a. SELECT * FROM users WHERE id = 'bob.hr'
    |     --> Gets `attributes` JSON: {"region": "US", ...}
    |
    | 3b. SELECT d.department_code FROM departments d
    |     JOIN user_departments ud ON ... WHERE ud.user_id = 'bob.hr'
    |     --> Gets `departments`: ["HR"]
    |
    | 3c. SELECT r.role_name, r.metadata->'queues' FROM business_app_roles r
    |     JOIN user_business_app_roles ur ON ... WHERE ur.user_id = 'bob.hr' AND app_name = 'OneCMS'
    |     --> Gets `roles`: ["HR_SPECIALIST"]
    |     --> Gets `queues`: ["onecms-hr-review-queue", ...]
    |
    | 3d. **Constructs the full Principal object in memory**
    |
    v
+------------------+----------------+
| 4. Cerbos SDK Call (gRPC)         |
|    - entitlementSvc.check(         |
|        Principal.newInstance(...) |
|        Resource.newInstance(...)  |
|        "claim_task"               |
|      )                            |
+------------------+----------------+
                   |
                   | 5. Cerbos evaluates the Principal
                   |    and Resource against the YAML policies
                   |
                   v
+------------------+----------------+
| 6. Cerbos PDP (Policy Engine)     |
|    - Finds `resource.one-cms-workflow.yaml` |
|    - Checks the `claim_task` rule: |
|      "expr: request.resource.attr.currentTask.queue in request.principal.attr.queues" |
|    - Compares resource queue ('onecms-hr-review-queue') with principal queues (['onecms-hr-review-queue',...]) |
|    - Rule matches. Decision: ALLOW |
+------------------+----------------+
                   |
                   | 7. Cerbos returns `isAllowed: true`
                   |    to the Entitlement Service
                   |
                   v
+------------------+----------------+
| 8. Entitlement Service            |
|    - Receives the decision        |
+------------------+----------------+
                   |
                   | 9. HTTP 200 OK
                   |    Response Body:
                   |    { "allowed": true }
                   |
                   v
+------------------+----------------+
| 10. CMS Domain Service            |
|     - Receives "allowed" response |
|     - Proceeds to call the        |
|       Flowable Core Service       |
+-----------------------------------+
```

## Detailed Step-by-Step Explanation

1. **Action Initiated (CMS Service)**: A user, authenticated as bob.hr, clicks a button in the UI. This results in an API call to the CMS Service to claim a task. The CMS service knows the taskId, which is part of a processInstanceId, and that the action is claim_task.

2. **Call to Entitlement Service**: The CMS Service does not check permissions itself. It delegates. It makes a POST request to the Entitlement Service's /check endpoint. It sends the basic identifiers: the user's ID, the resource details (including all the case and task attributes), and the action.

3. **The Entitlement Service Builds the Context**: This is the most important step. The Entitlement Service receives the request and does the following:

   - **Fetches User Data**: It queries its users table to get Bob's static attributes (like his region).
   - **Fetches Relational Data**: It performs JOINs across its user_departments and user_business_app_roles tables to gather all of Bob's formal entitlements for the "OneCMS" application.
   - **Aggregates Data**: It combines all this information. It sees Bob is in the "HR" department. It sees he has the "HR_SPECIALIST" role. It reads the metadata JSON for that role and sees he is entitled to access the onecms-hr-review-queue.
   - **Constructs the Principal**: It assembles all of this into the rich Principal object I showed in the previous response. This object, which exists only in memory for this one request, represents a complete snapshot of "who Bob is" at this exact moment.

4. **The Cerbos SDK Call**: The Entitlement Service now has everything it needs. It uses the Cerbos SDK to make a gRPC call to the Cerbos PDP. It passes the Principal object, the Resource object (which was sent by the CMS Service), and the action.

5. **Cerbos Evaluates**: Cerbos receives the fully-formed request. It has no idea who "Bob" is in the real world. It only knows the data it was just given.

   - It finds the policy for the resource OneCMS::Process_CMS_Workflow_Updated.
   - It finds the rule for the claim_task action.
   - It evaluates the condition: `request.resource.attr.currentTask.queue in request.principal.attr.queues`.
   - It compares the queue value from the resource ("onecms-hr-review-queue") against the list of queues in the principal's attributes (["onecms-hr-review-queue", ...]).
   - The condition is true. The rule's effect is EFFECT_ALLOW.

6. **Decision is Made**: Cerbos's final decision is ALLOW.

7. **Response to Entitlement Service**: Cerbos sends the true result back to the Entitlement Service.

8. **Response to CMS Service**: The Entitlement Service wraps this boolean in a simple JSON response `{"allowed": true}` and sends it back to the CMS Service.

9. **Action Proceeds**: The CMS Service receives the "allowed" response and proceeds with the business logic: it calls the Flowable Core Workflow Service to actually claim the task. If it had received false, it would have immediately returned a 403 Forbidden error to the user.

## Summary

- **Principal in Cerbos?** No. Principals are constructed on-the-fly for every check.
- **Check from Metadata?** Yes, exactly. The Entitlement Service uses the metadata from your database (roles, departments, and the JSON metadata on the roles table) to build the Principal object.
- **The Flow**: The flow is always Client -> Asks Entitlement Service -> Entitlement Service Builds Context from DB -> Entitlement Service Asks Cerbos -> Cerbos Decides -> Entitlement Service Relays Decision -> Client Enforces

The Core Principle: Derived Roles are Dynamic Aliases
Think of a derived role not as a role you assign to a user in your database, but as a temporary, in-memory label that Cerbos gives to a principal during a single check.

It answers the question:

"During this specific check, does this user qualify as a case_department_member for this particular resource?"
The user never "has" the case_department_member role in any persistent way. They either meet the criteria to earn that label for a split second, or they don't.

The Full Authentication & Authorization Flow (with Derived Roles)
Let's use a clear, real-world scenario from your application:

User: bob.hr (A standard HR specialist).
Action: bob.hr is trying to read a specific case.
Resource: case-123, which has been classified and assigned to the HR department.
Flow Diagram: Authorization for "Read Case" using a Derived Role
code
Code
+-----------------------------------+
| 1. CMS Domain Service             |
|    - User 'bob.hr' (with JWT)     |
|      requests to read case-123    |
|    - Calls its EntitlementGateway |
+------------------+----------------+
                   |
                   | 2. POST /api/entitlements/check
                   |    Payload:
                   |    {
                   |      "principalId": "bob.hr",
                   |      "resource": {
                   |        "kind": "case",
                   |        "id": "case-123",
                   |        "attr": { "department_code": "HR" }
                   |      },
                   |      "action": "read"
                   |    }
                   |
                   v
+------------------+----------------+
| 3. Entitlement Service            |
|    (The Principal Builder)        |
+-----------------------------------+
    |
    | 3a. Fetches User, Departments, Roles, Queues for 'bob.hr'
    |     (This is the same as the previous flow)
    |
    | 3b. **Constructs the full Principal object in memory:**
    |     {
    |       "id": "bob.hr",
    |       "roles": ["HR_SPECIALIST"],
    |       "attr": { "departments": ["HR"], ... }
    |     }
    |
    v
+------------------+----------------+
| 4. Cerbos SDK Call (gRPC)         |
|    - entitlementSvc.check(...)    |
+------------------+----------------+
                   |
                   | 5. Cerbos receives the Principal and Resource
                   |
                   v
+------------------+-------------------------------------------------+
| 6. Cerbos PDP (Policy Engine) - **THE DERIVED ROLE MAGIC HAPPENS HERE** |
+-----------------------------------------------------------------+
    |
    | 6a. **Load Policy:** Finds `resource.case.yaml`.
    |
    | 6b. **Import Derived Roles:** Sees the line `importDerivedRoles: [one_cms_derived_roles]`
    |     and loads `derived_roles.one-cms.yaml`.
    |
    | 6c. **Evaluate Derived Roles:** Before checking the main rules, Cerbos evaluates the derived roles.
    |     - It looks at the `case_department_member` definition:
    |       `expr: request.resource.attr.department_code in request.principal.attr.departments`
    |     - It substitutes the real data:
    |       `expr: "HR" in ["HR"]`
    |     - **The expression evaluates to `true`!**
    |
    | 6d. **Temporarily Augment Principal:** For this check only, Cerbos adds the `case_department_member`
    |     label to Bob's principal. In Cerbos's memory, Bob now looks like this:
    |     `{ "id": "bob.hr", "roles": ["HR_SPECIALIST", "case_department_member"], ... }`
    |
    | 6e. **Evaluate Main Rules:** Now Cerbos checks the rules in `resource.case.yaml`. It finds this rule:
    |     - actions: ["read"]
    |       effect: EFFECT_ALLOW
    |       derivedRoles:
    |         - case_department_member
    |
    | 6f. **Match Rule:** Cerbos sees that the action is `read` and that Bob's temporarily augmented
    |     principal **has the `case_department_member` role**. The rule matches.
    |
    | 6g. **Final Decision:** The effect is `EFFECT_ALLOW`. The final decision is **ALLOW**.
    |
    v
+------------------+----------------+
| 7. Cerbos returns `isAllowed: true` |
+------------------+----------------+
                   |
                   v
+------------------+----------------+
| 8. Entitlement Service ...        |
| (Flow continues as before)        |
+-----------------------------------+
Detailed Step-by-Step Explanation
Request Initiation (CMS Service): bob.hr requests to view case-123. The CMS service loads the case from its database and sees it belongs to the HR department.
Call to Entitlement Service: The CMS Service calls the Entitlement Service's /check endpoint. This time, the resource.attr payload is crucial. It includes {"department_code": "HR"}. This is the data that will be used to evaluate the derived role.
Entitlement Service Builds the Principal: This step is identical to the previous explanation. The service queries its database and builds the rich Principal object for bob.hr, which includes his static roles (HR_SPECIALIST) and his attributes (including departments: ["HR"]).
Cerbos SDK Call: The Entitlement Service sends the fully-formed Principal and Resource objects to Cerbos.
Cerbos Receives the Context: Cerbos now has two pieces of data:
Principal: Bob, who is in the HR department.
Resource: The case, which is also in the HR department.
Cerbos Evaluates the Derived Role (The Magic):
Before it even looks at the main allow or deny rules, Cerbos processes the importDerivedRoles section.
It finds the case_department_member definition.
It takes the condition request.resource.attr.department_code in request.principal.attr.departments and plugs in the actual data from the request.
The condition becomes "HR" in ["HR"], which is true.
Because the condition is true, Cerbos says, "For this check, I will temporarily treat bob.hr as if he has the case_department_member role."
Now, with this temporary, augmented set of roles, Cerbos proceeds to evaluate the main rules in the resource.case.yaml policy.
It finds a rule that says "ALLOW read if the principal has the case_department_member derived role." Since Bob was just dynamically granted this role, the rule matches, and the final decision is ALLOW.
The Flow Completes: Cerbos returns true, and the CMS service grants access.
Why is this so powerful? RBAC + ABAC Combined
This flow beautifully combines Role-Based Access Control (RBAC) and Attribute-Based Access Control (ABAC):

RBAC: In your database, you assign a static role to Bob: HR_SPECIALIST. This is his primary, long-term identity.
ABAC: The derived role case_department_member is pure ABAC. It's not about who Bob is, but what the attributes of the situation are. The rule is based on a comparison of attributes: resource.department vs principal.department.
By using a derivedRole, you've created a clean, readable policy. You could have written the ABAC rule directly:

code
Yaml
# The "ugly" way without derived roles
- actions: ["read"]
  effect: EFFECT_ALLOW
  roles: ["user"]
  condition:
    match:
      expr: request.resource.attr.department_code in request.principal.attr.departments
This works, but it's less readable and harder to reuse. By creating the case_department_member alias, your main policy becomes a simple, RBAC-style check (derivedRoles: [case_department_member]), while the complex ABAC logic is neatly encapsulated and named in the derived_roles file. This makes your policies much easier to understand, maintain, and audit.