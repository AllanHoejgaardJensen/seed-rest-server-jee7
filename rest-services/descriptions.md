# Description

This "Sample REST JEE7" service is used to explore simple things around services.

## API Description

This API shows a very simple set of resources that emulate two micro-services which are: Account and Customer.

## General Principles
A semantic REST API is used and thus also HATEOAS in form of
[HAL](http://stateless.co/hal_specification.html).

The API can be versioned at the structural level by means of a HTTP Header and at the content level in each endpoint by means of the content-type. Every response holds the content-type with parameters and that is how client can know exactly what content version they get back. 

### Example of content versioning

If a client puts `application/hal+json` in  the `Accept` header - that client will get the newest edition of content produced with the default projection from the target endpoint of the request.

That might be `application/hal+json;concept=account;v=1` today and be `application/hal+json;concept=account;v=2` next week. 

The idea is that the client evaluates, whether the new version poses a problem or not and if the new version creates a problem the client puts `application/hal+json;concept=account;v=1` in the Accept header and get the old one (whilst looking at the change necessary to cope with `application/hal+json;concept=account;v=2`) - because not every effect on the clients can be found out at the model level (e.g. a field is expanded from 20 to 50 on the service side should not be a problem - unless the UI looks weird given that long values now can appear) therefore it is a good idea to lets users have a "something looks weird" action in order to know when a new version is accepted or not. Once a version is accepted by the client this version can be saved for that endpoint. This helps the client knowing what to put into the Accept header in the event of a new version making trouble on the client side. In order to make this fast and waterproof a server-side resource may be relevant for helping out clients that are initially started late.


## HTTP Request Headers used

A number of headers are used:

* `X-Log-Token` for correlating a number of activities between a service and its consumers.
* `X-Client-Version` for identification of a client version and its contract.
* `X-Service-Generation` to signal a non-current structure of the API (saves known redirects from a client perspective)
* `Accept` is used to signal what content the consumer wishes, the version and projection can be specified.

### Correlation between service (Server) and consumer (Client)

A `X-Log-Token` header is used to give a client the opportunity to relate a number of calls and activities to a context.

If the client includes a `X-Log-Token` header and a value associated with that, the value will be extended with a time tick initially to
ensure uniqueness. The original `X-Log-Token` is returned in every response from the service, the client must include the unique token received
in the response in the following requests if the correlation is still what the client wants.

If the client does not include a `X-Log-Token` the service creates a unique token and returns that in the response and the client can use that.

### Client Identifier

The `X-Client-Version` header is used for identification of the client version and is required in order for a client to successfully operate on
resources. The versions are numbered according to [semver-org](http://semver.org).

### Versions

There are two major forms of versioning: one is related to the structure of the API, the other is related to the contents in each endpoint in the API. 

The two different aspects are handled in each their dedicated fashion.

The `X-Service-Generation` HTTP header is used for signalling the version of the API structure instead of having the version as a part of the
baseURL.

The content-type includes version information and is returned in every response from the service. The content-type can do that in a couple of ways: 

* using `"_links": {"href": "..."}` with no `"type"` will point to the newest and current content at the referenced endpoint.
* using  `"_links": {"href": "...", "type": "application/hal+json;v=1"}` with `"type"` will point to the specified version at the referenced endpoint.

The client must know if a problem has occurred in a situation, where the contents from a service endpoint was updated in a way that this particular client could not cope with. Therefore it must know what version works and the HAL specification can be used to decorate the `"_links":` object with the version of the content that it understands. That lets the client include the understandable content-type as defined
in `"_links":` and include that as the value of the "Accept" header.

An example of such client side decorated response from a server, where the default and newest content-type are "overwritten" by the type for the
users:

    {
      "label": "Budget Account",
      "currency": "DKK",
      ...
      "_links": {
        "transactions": [{
            "href": "customers/1234-567890/transactions/987654321"
          }, {
            "href": "customers/1234-567890/transactions/987654322"
          }, {
            "href": "customers/1234-567890/transactions/987654323"
          }
        ],
        "users": {
          "primary": {
            "href": "users/hans-b-hansen-13-09-1234",
            "type": "application/hal+json;v=1"
          },
          "coUsers": [{
              "href": "users/frederikke-b-hansen-16-07-6789",
              "type": "application/hal+json;v=1"
            },{
              "href": "users/ulla-b-hansen-23-03-4567",
              "type": "application/hal+json;v=1"
            }]
        }
      }
    }

If a projection (a given view on e.g. the user is needed) that may be included in the content-type as well, and if a matching producer is
available in the service it will be used.

The version is referring to the structure and contents of the json response from a given endpoint. It is not the historical state of a user
object. Examples of the versions of content by value in the `Accept` header below:

* `"application/hal+json;concept=user;v=1"` for the complete user json in hal format
* `"application/hal+json;concept=user-basic;v=1"` for the basic user information in hal json format
* `"application/hal+json;concept=user-basic"` for the newest version of basic user information in hal format
* `"application/hal+json"` for the newest version of user information in hal format

## API Capability Set

In every API of a certain size a number of capabilities are used to provide a particular set of functionality.

This example contains the following capabilities:

- Selection: `select`
  - selects objects by attribute value(s)
- Sorting: `sort`
  - sorts objects descending or ascending by attribute(s)
- Temporal: `interval`
  - limits objects to those within a certain time frame
- Pagination: `elements`
  - specifies objects within in a range
- Filtering: `filter`
  - excludes or includes objects based on attribute(s)
- Composition: `embed`
  - includes "related" objects and projections into the response

These capabilities may be applied individually to endpoints in APIs.

The user of the API endpoint can see what capabilities are supported at each endpoint by looking for tags like select, sort, paginate etc.
The Swagger tags are used here to achieve an easy way to show the capabilities in each endpoint as can be seen below in the Swagger
specification.

Another perspective that is often seen in APIs is the use of technical keys (potentially UUIDs) which are semantically poor, but often seen as
a necessity for sensitive keys such as social security numbers. In order to avoid having such sensitive information leaked to logs etc. there is
a need for bringing these keys into the body of a request and a non-sensitive key is going to help.
The problem with an UUID'ish key is that the developer experience is not optimal. Therefore it would be a nice thing to get some form of
consensus on a derived capability like :

Sensitive Semantic ID deconstruction

- generation of non-sensitive semantic key for objects that has a sensitive semantic key in the form of something that has a better developer
  experience than e.g. UUIDs can offer.

### Selection API Capability

Selection by criteria is done using a Query Parameter called `select`.

The syntax is:

    select="<attribute>::<value>|<attribute>::<value>|..."

#### Simple Example

    https://banking.services.sample-bank.dk/customers?select="balance::100"

selects customers having an exact balance of 100.

#### Selecting an interval

    https://banking.services.sample-bank.dk/customers?select="balance::100+|balance::1000-"

selects customers having a balance between 100 and 1000 (both inclusive).

#### Selecting multiple objects

    https://banking.services.sample-bank.dk/customers?select="no::123456789|no::234567890"

selects the two customers having customer numbers "123456789" and "234567890".

### Sorting API Capability

Sorting is done using a `sort` Query Parameter. Sort order can be either ascending (default) or descending.

The syntax is:

    sort="<attribute>+/-|<attribute>+/-|..."

and is equivalent to: `sort="<attribute>::+/-|<attribute>::+/-|..."`.

#### Simple Example

    https://banking.services.sample-bank.dk/customers?sort=balance

sorts customers by ascending balance.

#### Sorting on two properties

    https://banking.services.sample-bank.dk/customers?select=balance|lastUpdate-

sorts customers by ascending balance and descending lastUpdate.

### Temporal API Capability

Temporal aspects are handled using the `interval` Query Parameter.

The syntax is:

    interval="<now/from/to/at/::+/-/#d/#/now>|<now/from/to/at/::+/-/#d/#>"

#### Example

    https://banking.services.sample-bank.dk/customers/1234-56789/transactions?interval="from::-14d|to::now"

returns the transactions from a specific customer within the last 14 days.

#### More examples

    https://banking.services.sample-bank.dk/customers/1234-56789/transactions?interval="from::1476449846|to::now"
    https://banking.services.sample-bank.dk/customers/1234-56789/transactions?interval="from::1476449846"
    https://banking.services.sample-bank.dk/customers/1234-56789/transactions?interval="at::1476449846"

All three return the transactions from a specific customer within the last day assuming it is friday the 14th of October 2016 UTC time.

### Pagination API Capability

Pagination of responses is obtained by using the Query parameter `elements` which signals the initial element and the last element to be part of
the response.

The syntax is:

    elements="<startingFrom>|<endingAt>"

both inclusive.

The maximum element count is 500.

#### Example:

    https://banking.services.sample-bank.dk/customers/1234-56789/transactions?elements="10|30"

returns elements 10 to 30.

### Filtering API Capability

The Query parameters `filter` is used for requesting a dynamic projection. The service is not obliged to be able to support this, but may return
the standard projection of the objects given for that endpoint. This can be used for discovery of what projections service consumers would like
to have and help evolving the API to stay relevant and aligned with the consumers use of the service.

The syntax is:

    filter="<attribute>::+/-|<attribute>::+/-"

* `+` means include
* `-` means exclude

#### Example of exclude

    https://banking.services.sample-bank.dk/customers/1234-56789?filter="balance::-|name::-"

ideally returns an customer object without balance and name attributes.

The service may however choose not to support this and return a complete object and not this sparse dynamic view.

#### Example of include

    https://banking.services.sample-bank.dk/customers/1234-56789?filter="balance::+|name::+"

ideally returns an customer object with only balance and name attributes.

The service may however choose not to support this and return a complete object and not this sparse dynamic view.

### Composition API Capability

Composition is about enabling the service consumers to compose objects. The query parameter `embed` signals that the consumer wants a certain
related object included as a part of the response if possible.

The syntax is:

    embed="<concept>::<projection>|<concept>::<projection>|..."

#### Example:

    https://banking.services.sample-bank.dk/customers/1234-56789?embed="transaction::list|owner::sparse"

ideally returns a json response including `_links` and `_embedded` objects inside the response containing either a map or array of transactions
with links in the `_links` object and the desired projection in the `_embedded` object for both owner and transactions.

The service can choose to return just the customers including links to transactions under the `_links` object as this is allowed by HAL.
The query parameter `embed` can be used for evolving the service to match the desires of consumers - if many consumers have the same wishes for
what to embed, the service provider should consider including more in the responses and endure the added coupling between this service and the
service that delivers the embedded information. This coupling should of course not be synchronous.

### Sensitive Id decomposition

The creation of an id can be challenging especially if the true semantic id is protected by law, which is the case for persons. Therefore either
a UUID is suggested or a semi-semantic approach like firstName-middleName-sirName-dayInMonth-MonthInYear-Sequence, that allows for a human
readable (yet not revealing) id for a person.

Other suggested methods have been to create a hash(sensitive semantic key), which might work but will be vulnerable to a brute force
re-engineering effort. The response to that is often to salt it, that is salt(hash(sensitive semantic key)), and that is ok but it seems merely
to be a very difficult way to create a UUID. This means we have a key that is developer unfriendly - at least compared to the more human
readable key consisting of recognizable fragments from the real world.

The suggested approach is firstname-middlename-familyname-ddMM-sequencenumber

#### Example:

- `hans-p-hansen-0112` the initial created Hans P Hansen born on the 1st of December
- `hans-p-hansen-0112-1` the second created Hans P Hansen born on the 1st of December
- `hans-p-hansen-0112-94` the 95th created Hans P Hansen born on the 1st of December
- `mike-hansson-0309` the initially created Mike Hansson born on the 3rd of September

## Resilience

### Limits

A service might be busy for some reason and need to tell the client that. In fact it is trying to say "Be patient - I am rather busy right now".
Therefore the server returns a `503` error code (see response codes below) with a *Retry-After* header stating the time when it is expected that
the server is no longer busy and can serve a consumer again.

If the server-side intercepts the consumer it may choose to return a `429` *Too many Requests* with a response stating that "You are limited to
# requests per hour per `access_token` or `client_id` in total per `time unit` overall, which clients can follow from the response headers `X-RateLimit-*`.

## Responses

The responses from calling resources in the API adheres to the HTTP specification and thus the status code and the headers used are found in
that specification. All responses includes a X-Log-Token which is either the client defined correlation ID or a generated 36 characters long UUID 
that the service generates and the client can use if needed to further requests. The recommendation is that clients starts their version of the 
X-Log-Token with a unique prefix in order for clients getting access to the result that is concerned with that particular client in an easy way.
The following Headers are included in responses:

- `Content-Type` - the concrete content-type such as application/hal+json;concept=account;v=2
- `(Content-Encoding not in the seed yet)` - is the response e.g. compressed, gzipped etc.
- `ETag` - the unique hash for the values of the content copy
- `Expires` - the time the validity for the resource content "copy" expires
- `Last Modified` - the time recorded for the last modification of the resource
- `Cache Control` - the audience, state and cache ability of the copy
- `X-Log-Token` - a 36 character string 
- `X-RateLimit-Limit` - Request limit per minute
- `X-RateLimit-Limit24h` - Request limit per 24h
- `X-RateLimit-Remaining` - Requests left for the domain/resource for the 24h (locally determined)
- `X-RateLimit-Reset` - The remaining window before the rate limit resets in UTC epoch seconds


### Status Codes

Information on status code and headers are found under:

- Status codes: <https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html>
- Headers: <https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html>

A couple of response codes will be described here as inspiration, but the most important thing is to work with the protocol and understand what
the status codes mean and how they fit into the current situation in the API. It should signal explicitly what the client can expect in every
situation in order to make the API as developer friendly as possible. The recommended set of responses will vary with your needs. 

    A tool is under development that aims at ensuring that the responses defined in your API makes it possible to develop services with a high velocity without making clients suffer when the API is evolving.

#### 200 OK

The `200 OK` response signals everything went ok and there will usually be a response that contains a body corresponding to the initiating
request's `Accept` header.

#### 201 Created

The `201 Created` follows after a successful `POST` or `PUT` and states where the newly resource was created in a `Location` header.

#### 202 Accepted

The `202 Accepted` response signals that the request was understood and the response will follow later. The `Location` header should state where
information can be obtained later. To be nice, the `Retry-After` header may issue a time frame for when it makes sense to ask for a status again:

    Location: http://get/the/new/status/location
    Retry-After: 30
  
If the endpoint may take longer than e.g. 50ms it would be sound advice to include 202 from the beginning in the API description.
It is important to notice that the introduction of a 202 response would cause consumers to perceive that as a breaking change for the API
and thus the service generation must be increased and therefore consumers of the "previous" version without the 202 must include the header
`X-Service-Generation` in the consumer requests.

If you did not include that response as a part of your API before starting moving your resources, clients will typically not 
have that included in their programming model and thus experience this as a breaking change.

#### 203 Non Authoritative

The `203 Non Authoritative` can be used to indicate towards clients that the content is either entirely or partly not 
origin from the endpoint itself. This means that information can cone from 3rd parties. 
(Btw. it is important if you use that scenario - that you know your right to actually pass information on from that 3rd party.)

#### 204 No Content

The `204 No Content` can be used to indicate towards clients that a resource exists and the operation went well, and 
metadata may be returned, the body is however empty.


#### 301 Moved Permanently

The `301` is issued if a resource is no longer at the place where it used to be. This is one of the important response codes
to have as a part of your API specification, as this allows you to move endpoints as the service evolves. If you did not include
that response as a part of your API before starting moving your resources, clients will typically not have that included in their
programming model and thus experience this as a breaking change.

For instance this could be a resource that has moved to a different part of the API. In that case a `301` is returned with a `Location` header
containing the new position:

    Location: http://this/is/the/new/location

### 304 Not Modified
The `304` is worth considering for service offload as this allows the service to operate with a lower server capacity.

#### 307 Temporary Redirect

The `307` is issued if a resource is no longer at the place where it used to be for a while.

For instance this could be a resource that has moved to a different part of the API. In that case a `307` is returned with a `Location` header
containing the new position:

    Location: http://this/is/the/new/location

#### 400 Bad Request

The `400` response states that this request was wrong and should not be retried.

#### 401 Unauthorized

The `401` response states that this request did not have user authentication and that usually means that the client needs to either have a
contract for that resource, authenticate the user, renew a token or ... in order to get access to the requested resource.

#### 403 Forbidden

The `403` response states that this request carries user authentication but does not have sufficient authorizations to access the resource.

#### 404 Not Found

The `404` response states that the resource requested did not exist.

#### 409 Conflict

The `409` response states that the request for the resource is resulting in a form of conflict, which the client must resolve before retrying.
This could be trying to POST changes to an object that would cause the object to be in an erroneous state.

#### 410 Gone

The `410` response states that the resource is gone and this allows for taking the deprecation of a resource to the next level.

#### 412 Precondition Failed

The `412` response states that a precondition failed, this could be included in the `400`, however this is more precise in the event of a precondition failing.

#### 415 Unsupported Media Type

The `415` response states that a response was asked for using a content-type, which was not supported.

#### 429 Too Many Requests

The `429` response states that too much load is added from the client side into the service and the client is requested to limits the number 
of requests as the limits has been reached. A Retry-After header is part of the response:

    Retry-After: Sat, 31 Dec 2016 23:59:59 GMT

or

    Retry-After: 1200

#### 501 Not Implemented
  
  The `501` response states that the resource was not yet implemented.


#### 503 Service Unavailable

The `503` response states that the server for some reason is unavailable:

    Retry-After: Sat, 31 Dec 2016 23:59:59 GMT

or

    Retry-After: 120

#### 505 HTTP Version Not Supported
  
  The `505` response states that the protocol version is either not supported anymore (e.g. HTTP 1) 
  or is not yet supported (e.g. HTTP 2).


## Service terms

The terms of using the service is as follows, the contract found at <https://sample-rest-jee7.dk/services/contracts/jee7-sample> states the general terms
for using this service. Consumers can create their own individual contract and terms for usage at <https://sample-rest-jee7.dk/services/consumers>.
