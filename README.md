# guacamole-auth-hmac

## Update

## Description

This project is a plugin for [Guacamole](http://guac-dev.org), an HTML5 based
remote desktop solution supporting VNC/RFB, RDP, and SSH.

This plugin is an _authentication provider_ that enables stateless, on-the-fly
configuration of remote desktop connections that are authorized using a
pre-shared key. It is most appropriate for scenarios where you have an existing
user authentication & authorization mechanism.

## Building

guacamole-auth-hmac uses Maven for managing builds. After installing Maven you can build a
suitable jar for deployment with `mvn package`.

The resulting jar file will be placed in `target/guacamole-auth-hmac-<version>.jar`.

## Deployment & Configuration

**Warning** This plugin relies on API's introduced in Guacamole 0.8.3, so you must be running
at least that version before using this plugin.

Copy `guacamole-auth-hmac.jar` to the location specified by
[`lib-directory`][config-classpath] in `guacamole.properties`.

`guacamole-auth-hmac` adds two new config keys to `guacamole.properties`:

 * `secret-key` - The key that will be used to verify URL signatures.
    Whatever is generating the signed URLs will need to share this value.
 * `timestamp-age-limit` - A numeric value (in milliseconds) that determines how long
    a signed request should be valid for.


[config-classpath]: http://guac-dev.org/doc/gug/configuring-guacamole.html#idp380240

## Usage

[Guacamole POST - reason for some changes](https://glyptodon.org/jira/browse/GUAC-1102?jql=project%20%3D%20GUAC%20AND%20resolution%20%3D%20Unresolved%20AND%20priority%20%3D%20Major%20ORDER%20BY%20key%20DESC)

 * `id`  - A connection ID that must be unique per user session.
 * `timestamp` - A unix timestamp in milliseconds. (E.G. `time() * 1000` in PHP).
   This is used to prevent replay attacks.
 * `signature` - The [request signature][#request-signing]
 * `guac.protocol` - One of `vnc`, `rdp`, or `ssh`.
 * `guac.hostname` - The hostname of the remote desktop server to connect to.
 * `guac.port` - The port number to connect to.
 * `guac.username` - (_optional_)
 * `guac.password` - (_optional_)
 * `guac.*` - (_optional_) Any other configuration parameters recognized by
    Guacamole can be by prefixing them with `guac.`.

## Request Signing

Requests must be signed with an HMAC, where the message content is generated
from the request parameters as follows:

 1. The parameters `timestamp`, and `guac.protocol` are concatenated.
 2. For each of `guac.username`, `guac.password`, `guac.hostname`, and `guac.port`;
    if the parameter was included in the request, append it's unprefixed name
    (e.g. - `guac.username` becomes `username`) followed by it's value.

## POST
Using the php form example, parameters can be POSTed to `/guacamole/api/tokens` to authenticate. The response is then sent as JSON and contains `authToken` which is then used to login: `guacamole/#/client/c/(id)?token=(authToken)`
